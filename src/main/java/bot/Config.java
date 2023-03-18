package bot;

import java.util.List;
import java.util.Optional;

public record Config(
        String tgToken,
        String botName,
        String openAiToken,
        String openAiUrl,
        String openAiModel,
        Double openAiTemperature,
        Double openAiTopP,
        List<String> usersWhitelist,
        Optional<String> chatGptsRole,
        Integer chatHistorySize
) {
}