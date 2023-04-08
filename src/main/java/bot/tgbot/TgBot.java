package bot.tgbot;

import bot.Config;
import bot.audioconverter.Converter;
import bot.chatgpt.ChatGptApi;
import bot.chatgpt.ChatGptMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.facilities.filedownloader.TelegramFileDownloader;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Voice;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TgBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TgBot.class);

    private final ConcurrentHashMap<Long, ArrayList<ChatGptMessage>> chatsHistoryCache = new ConcurrentHashMap<>();
    private final Config config;
    private final ChatGptApi api;
    private final TelegramFileDownloader fileDownloader;


    public TgBot(Config config, ChatGptApi api, TelegramFileDownloader fileDownloader) {
        super(config.tgToken());
        this.config = config;
        this.api = api;
        this.fileDownloader = fileDownloader;
    }

    private SendMessage mkMessageToSend(Long chatId, String aiResponse, Optional<Integer> replyToMessageId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(aiResponse);
        replyToMessageId.ifPresent(message::setReplyToMessageId);
        return message;
    }

    private CompletableFuture<Message> trySendMessage(SendMessage message) {
        try {
            return executeAsync(message);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IllegalStateException("!!!!!"));
        }
    }

    private List<ChatGptMessage> assembleChatHistory(Long chatId, String latestMessage) {
        var chatHistory = new ArrayList<ChatGptMessage>();

        config.chatGptsRole().ifPresent(role -> chatHistory.add(new ChatGptMessage("system", role)));

        var cached = chatsHistoryCache.get(chatId);
        if (cached != null) {
            chatHistory.addAll(cached);
        }

        chatHistory.add(new ChatGptMessage("user", latestMessage));
        return chatHistory;
    }

    private void updateChatHistory(Long chatId, String usersMessage, String chatGptsResponse) {
        chatsHistoryCache.compute(chatId, (key, nullableValue) -> {
            var value = nullableValue == null ? new ArrayList<ChatGptMessage>() : nullableValue;

            value.add(new ChatGptMessage("user", usersMessage));
            value.add(new ChatGptMessage("assistant", chatGptsResponse));

            while (value.size() > config.chatHistorySize()) {
                value.remove(0);
            }

            return value;
        });
    }

    private String extractUsername(User user) {
        return user.getUserName() != null ? user.getUserName() : user.getId().toString();
    }

    private boolean isInWhitelist(User user) {
        if (config.usersWhitelist().isEmpty())
            return true;
        if (config.usersWhitelist().stream().allMatch(str -> str.equals(user.getUserName()))) {
            return true;
        } else {
            logger.info("Ignoring message from {}, since they are not in the whitelist", extractUsername(user));
            return false;
        }
    }

    private void handleCommandMessage(User user, Long chatId, String message) {
        switch (message) {
            case "/clean" -> {
                logger.info("Cleaning chat history for {}", extractUsername(user));
                chatsHistoryCache.remove(chatId);
                trySendMessage(mkMessageToSend(chatId, "Cache cleared", Optional.empty()));
            }
            case "/config" -> {
                logger.info("Responding with config to {}", extractUsername(user));
                trySendMessage(mkMessageToSend(chatId, config.prettyPrint(), Optional.empty()));
            }
            case "/start" -> {
                logger.info("Responding with introduction message to {}", extractUsername(user));
                handleCommonMessage(user, chatId, INTRODUCTION_PROMPT);
            }
            default -> logger.info("Unknown command from {}: {}", extractUsername(user), message);
        }
    }

    private void handleCommonMessage(User user, Long chatId, String message) {
        logger.info("Got message from {}, in chat {}: {}", extractUsername(user), chatId, message);
        api.getAiResponse(assembleChatHistory(chatId, message))
                .thenApplyAsync(response -> {
                    updateChatHistory(chatId, message, response);
                    return response;
                })
                .thenApplyAsync(response -> mkMessageToSend(chatId, response, Optional.empty()))
                .thenComposeAsync(this::trySendMessage);
    }

    private void handleVoiceMessage(User user, Long chatId, Voice voice, Integer messageId) {
        logger.info("Got audio message from {}, in chat {}", extractUsername(user), chatId);
        File ogaAudio = null;
        File wavAudio = null;
        try {
            var voiceMessageInfo = execute(new GetFile(voice.getFileId()));
            ogaAudio = fileDownloader.downloadFile(voiceMessageInfo.getFilePath());
            wavAudio = new Converter().convertOgaToWav(ogaAudio);
            api.parseVoice(Files.readAllBytes(wavAudio.toPath())).thenApplyAsync(parsedMessage -> {
                        var parsedMessageResponse = mkMessageToSend(chatId, parsedMessage, Optional.of(messageId));
                        trySendMessage(parsedMessageResponse);
                        handleCommonMessage(user, chatId, parsedMessage);
                        return true;
                    }
            );
        } catch (Exception e) {
            logger.info("Failure", e);
        } finally {
            if (ogaAudio != null) ogaAudio.delete();
            if (wavAudio != null) wavAudio.delete();
        }

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            logger.info("Update {} doesn't have message, skipping", update.getUpdateId());
            return;
        }
        if (!isInWhitelist(update.getMessage().getFrom())) {
            return;
        }
        var chatId = update.getMessage().getChatId();
        var user = update.getMessage().getFrom();
        if (update.getMessage().hasText()) {
            var message = update.getMessage().getText();
            if (message.startsWith("/")) {
                handleCommandMessage(user, chatId, message);
            } else {
                handleCommonMessage(user, chatId, message);
            }
        } else if (update.getMessage().hasVoice()) {
            var voice = update.getMessage().getVoice();
            handleVoiceMessage(user, chatId, voice, update.getMessage().getMessageId());
        }
    }

    @Override
    public String getBotUsername() {
        return config.botName();
    }

    @Override
    public String getBotToken() {
        return config.tgToken();
    }

    private static final String INTRODUCTION_PROMPT =
            "Introduce yourself and offer your service. Be concise";

}
