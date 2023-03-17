package bot;

import bot.chatgpt.ChatGptApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Optional;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static String readMandatoryEnv(String name) {
        final var value = System.getenv().get(name);
        if (value == null) {
            logger.error("Env variable " + name + " is not set");
            System.exit(1);
            return "";
        } else {
            return System.getenv().get(name);
        }
    }

    private static Optional<String> readOptionalEnv(String name) {
        return Optional.ofNullable(System.getenv().get(name));
    }

    public static void main(String[] args) {
        try {
            final var tgToken = readMandatoryEnv("TG_APIKEY");
            final var openAiToken = readMandatoryEnv("OPENAI_APIKEY");
            final var modelName = readOptionalEnv("MODEL_NAME").orElse("gpt-3.5-turbo");
            final var config = new Config(
                    tgToken,
                    "ChatGPT bot",
                    openAiToken,
                    "https://api.openai.com/v1",
                    modelName
            );
            logger.info("Starting ChatGPT Telegram bot");
            logger.info("Configuration: " + config);

            final var api = new ChatGptApi(config);

            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new TgBot(config, api));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}