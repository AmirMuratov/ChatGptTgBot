package bot.chatgpt;

import java.util.List;

record CompletionRequest(
        String model,
        List<ChatGptMessage> messages,
        Double temperature,
        Double top_p
) {
}