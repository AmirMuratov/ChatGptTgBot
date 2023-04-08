package bot.chatgpt;

import bot.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

    private HttpRequest mkCompletionRequest(CompletionRequest request) {

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
        logger.info("Request body: " + body);

        return httpRequest;
    }

    private HttpRequest mkAudioParseRequest(byte[] wavAudio) {
        var httpEntity = MultipartEntityBuilder.create()
                .addPart("model", new StringBody("whisper-1", ContentType.DEFAULT_TEXT))
                .addBinaryBody("file", wavAudio, ContentType.DEFAULT_BINARY, "speech.wav")
                .build();

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
            httpEntity.writeTo(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final var httpRequest = HttpRequest
                .newBuilder()
                .header("Content-Type", httpEntity.getContentType().getValue())
                .header("Authorization", "Bearer " + config.openAiToken())
                .uri(URI.create(config.openAiUrl() + "/audio/transcriptions"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(o.toByteArray()))
                .build();
        logger.info("Request: " + httpRequest);
        return httpRequest;
    }


    //todo 1 generic method instead of parseCompletionBody and parseTranscriptionBody
    private Optional<CompletionResponse> parseCompletionBody(HttpResponse<String> body) {
        try {
            logger.info("Status code: {}", body.statusCode());
            logger.info("Response: {}", body.body());
            final var mapper = new ObjectMapper();
            final var parsedResponse = mapper.readValue(body.body(), CompletionResponse.class);
            return Optional.of(parsedResponse);
        } catch (Exception e) {
            logger.error("Failed to parse body: " + body.body(), e);
            return Optional.empty();
        }
    }

    private Optional<TranscriptionCompletionResponse> parseTranscriptionBody(HttpResponse<String> body) {
        try {
            logger.info("Status code: {}", body.statusCode());
            logger.info("Response: {}", body.body());
            final var mapper = new ObjectMapper();
            final var parsedResponse = mapper.readValue(body.body(), TranscriptionCompletionResponse.class);
            return Optional.of(parsedResponse);
        } catch (Exception e) {
            logger.error("Failed to parse body: " + body.body(), e);
            return Optional.empty();
        }
    }

    public CompletableFuture<String> getAiResponse(List<ChatGptMessage> chat) {
        final var request = new CompletionRequest(config.openAiModel(), chat, config.openAiTemperature(), config.openAiTopP());
        final var httpRequest = mkCompletionRequest(request);

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(this::parseCompletionBody)
                .thenApplyAsync(maybeResponse -> maybeResponse.map(response -> response.choices().get(0).message().content()).orElse("oops"));
    }

    public CompletableFuture<String> parseVoice(byte[] wavAudio) {
        final var httpRequest = mkAudioParseRequest(wavAudio);
        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(this::parseTranscriptionBody)
                .thenApplyAsync(maybeResponse -> maybeResponse.map(TranscriptionCompletionResponse::text).orElse("oops"));
    }

}
