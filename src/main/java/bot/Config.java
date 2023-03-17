package bot;

public record Config(
        String tgToken,
        String botName,
        String openAiToken,
        String openAiUrl,
        String openAiModel
) {
}