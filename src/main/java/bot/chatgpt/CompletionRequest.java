package bot.chatgpt;

import java.util.List;

record CompletionRequest(
        String model,
        List<Message> messages
) {
}