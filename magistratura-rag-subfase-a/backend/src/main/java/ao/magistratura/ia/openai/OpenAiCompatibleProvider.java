package ao.magistratura.ia.openai;

import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.ia.IaQuotaState;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.ChatRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Provider compatível com API OpenAI Chat Completions.
 * Serve OpenAI oficial e proxies (OpenRouter, etc.) via base-url + api-key.
 * Activado com app.ia.provider=openai (ou openrouter com as mesmas props).
 */
@Component
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "openai")
public class OpenAiCompatibleProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final double temperature;
    private final int maxTokens;
    private final IaQuotaState quotaState;

    /**
     * Construtor Spring (app.ia.provider=openai) e também usado por OpenRouterProvider via {@code new}.
     * Os {@code @Value} só aplicam quando o bean é criado pelo Spring.
     */
    public OpenAiCompatibleProvider(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-4o-mini}") String model,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.timeout-seconds:120}") long timeoutSeconds,
            @Value("${app.openai.temperature:0.25}") double temperature,
            @Value("${app.openai.max-tokens:2048}") int maxTokens,
            IaQuotaState quotaState
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.quotaState = quotaState;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    private String label() {
        if (baseUrl != null && baseUrl.contains("groq")) {
            return "groq";
        }
        if (baseUrl != null && baseUrl.contains("openrouter")) {
            return "openrouter";
        }
        return "openai";
    }

    @Override
    public String nome() {
        return "OpenAI-compatible (" + model + ")";
    }

    @Override
    public boolean disponivel() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String chat(List<ChatMessage> mensagens) {
        if (!disponivel()) {
            throw new IAIndisponivelException("OpenAI: app.openai.api-key não configurada");
        }
        try {
            String body = buildBody(mensagens, false);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                log.error("OpenAI HTTP {}: {}", response.statusCode(), truncate(response.body()));
                if (response.statusCode() == 429 && quotaState != null) {
                    Long retry = IaQuotaState.parseRetryAfterSeconds(response.headers());
                    quotaState.recordRateLimited(label(), 429, retry,
                            "Limite da API atingido. Aguarda e tenta novamente.");
                    throw new IAIndisponivelException(
                            "Limite da API atingido (HTTP 429). Tenta dentro de momentos.");
                }
                throw new IAIndisponivelException(label() + " respondeu HTTP " + response.statusCode());
            }
            if (quotaState != null) {
                quotaState.recordSuccess(label());
            }
            return extractContent(response.body());
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new IAIndisponivelException("Falha ao contactar OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(List<ChatMessage> mensagens,
                           Consumer<String> onToken,
                           Consumer<String> onComplete,
                           Consumer<Throwable> onErro) {
        if (!disponivel()) {
            onErro.accept(new IAIndisponivelException("OpenAI: app.openai.api-key não configurada"));
            return;
        }
        try {
            String body = buildBody(mensagens, true);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                if (response.statusCode() == 429 && quotaState != null) {
                    Long retry = IaQuotaState.parseRetryAfterSeconds(response.headers());
                    quotaState.recordRateLimited(label(), 429, retry, "Limite da API (stream) atingido.");
                }
                onErro.accept(new IAIndisponivelException(label() + " stream HTTP " + response.statusCode() + ": " + truncate(err)));
                return;
            }

            StringBuilder full = new StringBuilder();
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JsonNode node = mapper.readTree(data);
                        JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                        if (delta.isTextual()) {
                            String t = delta.asText();
                            full.append(t);
                            onToken.accept(t);
                        }
                    } catch (Exception ignore) {
                        // linha SSE parcial
                    }
                }
            }
            onComplete.accept(full.toString());
        } catch (Throwable t) {
            onErro.accept(t);
        }
    }

    private String buildBody(List<ChatMessage> mensagens, boolean stream) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);
        root.put("max_tokens", maxTokens);
        root.put("stream", stream);
        ArrayNode msgs = root.putArray("messages");
        for (ChatMessage m : mensagens) {
            ObjectNode o = msgs.addObject();
            String role = switch (m.role()) {
                case SYSTEM -> "system";
                case ASSISTANT -> "assistant";
                default -> "user";
            };
            o.put("role", role);
            o.put("content", m.content() == null ? "" : m.content());
        }
        return mapper.writeValueAsString(root);
    }

    private String extractContent(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isTextual() ? content.asText() : "";
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
