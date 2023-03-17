package bot.chatgpt;

import bot.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ChatGptApi {

    private final HttpClient httpClient;
    private final Config config;

    private static final Logger logger = LoggerFactory.getLogger(ChatGptApi.class);

    public ChatGptApi(Config config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    private HttpRequest mkRequest(CompletionRequest request) {

        final var mapper = new ObjectMapper();
        String body;
        try {
            body = mapper.writeValueAsString(request);
        } catch (Exception e) {
            body = "";
        }
        final var httpRequest = HttpRequest
                .newBuilder()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.openAiToken())
                .uri(URI.create(config.openAiUrl() + "/chat/completions"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        logger.info("Request: " + httpRequest);
        logger.info("Request headers: " + httpRequest.headers());
        logger.info("Request body: " + body);

        return httpRequest;
    }

    private Optional<CompletionResponse> parseBody(HttpResponse<String> body) {
        try {
            final var mapper = new ObjectMapper();
            final var parsedResponse = mapper.readValue(body.body(), CompletionResponse.class);
            return Optional.of(parsedResponse);
        } catch (Exception e) {
            logger.error("Failed to parse body: " + body.body(), e);
            return Optional.empty();
        }
    }

    public CompletableFuture<String> getResponse(String message) {
        final var request = new CompletionRequest(config.openAiModel(), List.of(new Message("user", message)));
        final var httpRequest = mkRequest(request);

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(this::parseBody)
                .thenApplyAsync(maybeResponse -> maybeResponse.map(response -> response.choices().get(0).message().content()).orElse("oops"));
    }

}
