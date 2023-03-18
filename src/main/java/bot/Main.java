package bot;

import bot.chatgpt.ChatGptApi;
import bot.tgbot.TgBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.Arrays;
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

    private final static String BOT_NAME = "ChatGPT bot";
    private final static String OPEN_AI_URL = "https://api.openai.com/v1";
    private final static Integer CHAT_HISTORY_SIZE = 20;


    private static Config readConfigFromEnv() {
        final var tgToken = readMandatoryEnv("TG_APIKEY");
        final var openAiToken = readMandatoryEnv("OPENAI_APIKEY");
        final var modelName = readOptionalEnv("OPENAI_MODEL_NAME")
                .orElse("gpt-3.5-turbo");
        final var openAiTemperature = readOptionalEnv("OPENAI_TEMPERATURE")
                .map(Double::parseDouble)
                .orElse(1.);
        final var openAiTopP = readOptionalEnv("OPENAI_TOP_P")
                .map(Double::parseDouble)
                .orElse(1.);
        final var usersWhitelist = readOptionalEnv("WHITELIST")
                .filter(str -> !str.isBlank())
                .map(str -> Arrays.asList(str.split("\\s*,\\s*")))
                .orElse(new ArrayList<>());
        final var chatGptsRole = readOptionalEnv("CHAT_GPT_PROMPT").filter(str -> !str.isBlank());

        return new Config(
                tgToken,
                BOT_NAME,
                openAiToken,
                OPEN_AI_URL,
                modelName,
                openAiTemperature,
                openAiTopP,
                usersWhitelist,
                chatGptsRole,
                CHAT_HISTORY_SIZE
        );
    }

    public static void main(String[] args) {
        try {
            final var config = readConfigFromEnv();

            logger.info("Starting ChatGPT Telegram bot");
            logger.info("Configuration: " + config.prettyPrint());

            final var api = new ChatGptApi(config);

            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new TgBot(config, api));
        } catch (Exception e) {
            logger.error("Failed to start Chat GPT bot", e);
        }
    }
}