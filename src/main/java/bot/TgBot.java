package bot;

import bot.chatgpt.ChatGptApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.CompletableFuture;

public class TgBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TgBot.class);
    private final Config config;
    private final ChatGptApi api;

    public TgBot(Config config, ChatGptApi api) {
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

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            logger.info("Got message: " + update.getMessage().getText());
            api.getResponse(update.getMessage().getText())
                    .thenApplyAsync(response -> mkMessageToSend(update.getMessage().getChatId(), response))
                    .thenComposeAsync(this::trySendMessage);
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
