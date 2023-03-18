package bot.chatgpt;

public record ChatGptMessage(
        String role,
        String content
) {
}