package bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

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

    public String prettyPrint() {
        final var mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        final var configWithoutSecrets = new Config(
                this.tgToken.substring(0, 5) + "***",
                this.botName,
                this.openAiToken.substring(0, 5) + "***",
                this.openAiUrl,
                this.openAiModel,
                this.openAiTemperature,
                this.openAiTopP,
                this.usersWhitelist,
                this.chatGptsRole,
                this.chatHistorySize
        );

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configWithoutSecrets);
        } catch (Exception e) {
            return "";
        }
    }

}