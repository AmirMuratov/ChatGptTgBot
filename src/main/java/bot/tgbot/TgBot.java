package bot.tgbot;

import bot.Config;
import bot.chatgpt.ChatGptApi;
import bot.chatgpt.ChatGptMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TgBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TgBot.class);

    private final ConcurrentHashMap<Long, ArrayList<ChatGptMessage>> chatsHistoryCache = new ConcurrentHashMap<>();
    private final Config config;
    private final ChatGptApi api;

    public TgBot(Config config, ChatGptApi api) {
        super(config.tgToken());
        this.config = config;
        this.api = api;
    }

    private SendMessage mkMessageToSend(Long chatId, String aiResponse) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(aiResponse);
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
                trySendMessage(mkMessageToSend(chatId, "Cache cleared"));
            }
            case "/config" -> {
                logger.info("Responding with config to {}", extractUsername(user));
                trySendMessage(mkMessageToSend(chatId, config.prettyPrint()));
            }
            default -> logger.info("Unknown command from {}: {}", extractUsername(user), message);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && isInWhitelist(update.getMessage().getFrom())) {
            var chatId = update.getMessage().getChatId();
            var message = update.getMessage().getText();
            var user = update.getMessage().getFrom();

            logger.info("Got message from {}, in chat {}: {}", extractUsername(user), chatId, message);

            if (message.startsWith("/")) {
                handleCommandMessage(user, chatId, message);
            } else {
                api.getResponse(assembleChatHistory(chatId, message))
                        .thenApplyAsync(response -> {
                            updateChatHistory(chatId, message, response);
                            return response;
                        })
                        .thenApplyAsync(response -> mkMessageToSend(chatId, response))
                        .thenComposeAsync(this::trySendMessage);
            }
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
}
