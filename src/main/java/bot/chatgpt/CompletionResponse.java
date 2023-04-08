package bot.chatgpt;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize
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
record TranscriptionCompletionResponse(String text) {
}


record Choice(
        Long index,
        ChatGptMessage message,
        String finish_reason
) {

}

record Usage(
        Long prompt_tokens,
        Long completion_tokens,
        Long total_tokens
) {
}
