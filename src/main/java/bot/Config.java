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
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            return "";
        }
    }

}