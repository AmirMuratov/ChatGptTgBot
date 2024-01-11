package bot.chatgpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
record CompletionResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices,
        String finish_reason,
        Usage usage
) {
}

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
record TranscriptionCompletionResponse(String text) {
}


@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
record Choice(
        Long index,
        ChatGptMessage message,
        String finish_reason
) {

}

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
record Usage(
        Long prompt_tokens,
        Long completion_tokens,
        Long total_tokens
) {
}
