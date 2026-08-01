package ao.magistratura.ia.gemini;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Provider Google Gemini (API generateContent).
 * Activado com app.ia.provider=gemini e app.gemini.api-key.
 */
@Component
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "gemini")
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final double temperature;
    private final int maxOutputTokens;
    private final IaQuotaState quotaState;

    public GeminiProvider(
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-2.0-flash}") String model,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${app.gemini.timeout-seconds:120}") long timeoutSeconds,
            @Value("${app.gemini.temperature:0.25}") double temperature,
            @Value("${app.gemini.max-output-tokens:2048}") int maxOutputTokens,
            IaQuotaState quotaState
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.quotaState = quotaState;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public String nome() {
        return "Gemini (" + model + ")";
    }

    @Override
    public boolean disponivel() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String chat(List<ChatMessage> mensagens) {
        if (!disponivel()) {
            throw new IAIndisponivelException("Gemini: app.gemini.api-key não configurada");
        }
        try {
            String body = buildBody(mensagens);
            String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                log.error("Gemini HTTP {}: {}", response.statusCode(), truncate(response.body()));
                if (response.statusCode() == 429) {
                    Long retry = IaQuotaState.parseRetryAfterSeconds(response.headers());
                    quotaState.recordRateLimited("gemini", 429, retry,
                            "Limite da API Gemini atingido. Aguarda e tenta novamente.");
                    throw new IAIndisponivelException(
                            "Limite da API Gemini atingido (HTTP 429). Tenta dentro de momentos.");
                }
                throw new IAIndisponivelException("Gemini respondeu HTTP " + response.statusCode());
            }
            quotaState.recordSuccess("gemini");
            return extractText(response.body());
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new IAIndisponivelException("Falha ao contactar Gemini: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(List<ChatMessage> mensagens,
                           Consumer<String> onToken,
                           Consumer<String> onComplete,
                           Consumer<Throwable> onErro) {
        // Gemini stream (SSE) pode ser adicionado depois; por agora fallback síncrono tokenizado.
        try {
            String full = chat(mensagens);
            if (full != null && !full.isEmpty()) {
                // Emitir em blocos para a UI SSE não ficar bloqueada sem feedback
                int step = Math.max(24, full.length() / 40);
                for (int i = 0; i < full.length(); i += step) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    String piece = full.substring(i, Math.min(full.length(), i + step));
                    onToken.accept(piece);
                }
            }
            onComplete.accept(full == null ? "" : full);
        } catch (Throwable t) {
            onErro.accept(t);
        }
    }

    private String buildBody(List<ChatMessage> mensagens) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        String system = null;
        for (ChatMessage m : mensagens) {
            if (m.role() == ChatRole.SYSTEM) {
                system = (system == null ? "" : system + "\n") + m.content();
                continue;
            }
            ObjectNode c = contents.addObject();
            c.put("role", m.role() == ChatRole.ASSISTANT ? "model" : "user");
            ArrayNode parts = c.putArray("parts");
            parts.addObject().put("text", m.content() == null ? "" : m.content());
        }
        if (system != null && !system.isBlank()) {
            ObjectNode sys = root.putObject("systemInstruction");
            sys.putArray("parts").addObject().put("text", system);
        }
        ObjectNode gen = root.putObject("generationConfig");
        gen.put("temperature", temperature);
        gen.put("maxOutputTokens", maxOutputTokens);
        return mapper.writeValueAsString(root);
    }

    private String extractText(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IAIndisponivelException("Gemini: resposta sem candidates");
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : parts) {
            if (p.has("text")) {
                sb.append(p.get("text").asText());
            }
        }
        return sb.toString();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
