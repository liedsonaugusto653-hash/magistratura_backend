package ao.magistratura.ia.ollama;

import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.ia.IaQuotaState;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
 * Cliente Ollama com limite de concorrência para produção.
 */
@Component
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);

    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OllamaConcurrencyLimiter concurrencyLimiter;
    private final IaQuotaState quotaState;
    private final double temperature;
    private final double topP;
    private final double repeatPenalty;
    private final int numPredict;
    private final int numPredictJson;
    private final int numCtx;

    public OllamaProvider(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.model}") String model,
            @Value("${app.ollama.timeout-seconds}") long timeoutSeconds,
            @Value("${app.ollama.temperature:0.25}") double temperature,
            @Value("${app.ollama.top-p:0.9}") double topP,
            @Value("${app.ollama.repeat-penalty:1.2}") double repeatPenalty,
            @Value("${app.ollama.num-predict:800}") int numPredict,
            @Value("${app.ollama.num-predict-json:2500}") int numPredictJson,
            @Value("${app.ollama.num-ctx:8192}") int numCtx,
            OllamaConcurrencyLimiter concurrencyLimiter,
            IaQuotaState quotaState
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.temperature = temperature;
        this.topP = topP;
        this.repeatPenalty = repeatPenalty;
        this.numPredict = numPredict;
        this.numPredictJson = numPredictJson;
        this.numCtx = numCtx;
        this.concurrencyLimiter = concurrencyLimiter;
        this.quotaState = quotaState;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    public String nome() {
        return "Ollama (" + model + ")";
    }

    @Override
    public boolean disponivel() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Ollama indisponível em {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public String chat(List<ChatMessage> mensagens) {
        acquire();
        try {
            String corpo = construirCorpoPedido(mensagens, false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                if (response.statusCode() == 429 && quotaState != null) {
                    quotaState.recordRateLimited("ollama", 429, 30L, "Ollama sobrecarregado.");
                }
                throw new IAIndisponivelException("O Ollama devolveu o estado HTTP " + response.statusCode());
            }
            if (quotaState != null) {
                quotaState.recordSuccess("ollama");
            }

            JsonNode raiz = objectMapper.readTree(response.body());
            return raiz.path("message").path("content").asText("");
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (IOException e) {
            throw new IAIndisponivelException("Não foi possível contactar o Ollama em " + baseUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IAIndisponivelException("Pedido ao Ollama interrompido", e);
        } finally {
            concurrencyLimiter.release();
        }
    }

    @Override
    public String chatJson(List<ChatMessage> mensagens) {
        acquire();
        try {
            String corpo = construirCorpoPedido(mensagens, false, true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                if (response.statusCode() == 429 && quotaState != null) {
                    quotaState.recordRateLimited("ollama", 429, 30L, "Ollama sobrecarregado.");
                }
                throw new IAIndisponivelException("O Ollama devolveu o estado HTTP " + response.statusCode());
            }
            if (quotaState != null) {
                quotaState.recordSuccess("ollama");
            }

            JsonNode raiz = objectMapper.readTree(response.body());
            return raiz.path("message").path("content").asText("");
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (IOException e) {
            throw new IAIndisponivelException("Não foi possível contactar o Ollama em " + baseUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IAIndisponivelException("Pedido ao Ollama interrompido", e);
        } finally {
            concurrencyLimiter.release();
        }
    }

    @Override
    public void chatStream(
            List<ChatMessage> mensagens,
            Consumer<String> onToken,
            Consumer<String> onComplete,
            Consumer<Throwable> onErro) {
        try {
            acquire();
        } catch (IAIndisponivelException e) {
            onErro.accept(e);
            return;
        }
        try {
            String corpo = construirCorpoPedido(mensagens, true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                onErro.accept(new IAIndisponivelException(
                        "O Ollama devolveu o estado HTTP " + response.statusCode()));
                return;
            }

            StringBuilder textoCompleto = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                String linha;
                while ((linha = reader.readLine()) != null) {
                    // Cliente SSE fechou → StreamingService interrompe esta thread
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("Ollama stream cancelado (cliente SSE desligado)");
                        try {
                            response.body().close();
                        } catch (Exception ignored) {
                        }
                        onErro.accept(new InterruptedException("Streaming do Ollama interrompido"));
                        return;
                    }
                    if (linha.isBlank()) {
                        continue;
                    }
                    JsonNode raiz = objectMapper.readTree(linha);

                    if (raiz.has("error")) {
                        onErro.accept(new IAIndisponivelException(
                                "Erro do Ollama: " + raiz.get("error").asText()));
                        return;
                    }

                    String fragmento = raiz.path("message").path("content").asText("");
                    if (!fragmento.isEmpty()) {
                        textoCompleto.append(fragmento);
                        onToken.accept(fragmento);
                    }

                    if (raiz.path("done").asBoolean(false)) {
                        break;
                    }
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                onErro.accept(new InterruptedException("Streaming do Ollama interrompido"));
                return;
            }
            onComplete.accept(textoCompleto.toString());
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                onErro.accept(new InterruptedException("Streaming do Ollama interrompido"));
                return;
            }
            onErro.accept(new IAIndisponivelException(
                    "Não foi possível contactar o Ollama em " + baseUrl, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onErro.accept(e);
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                onErro.accept(new InterruptedException("Streaming do Ollama interrompido"));
                return;
            }
            onErro.accept(e);
        } finally {
            concurrencyLimiter.release();
        }
    }

    private void acquire() {
        try {
            concurrencyLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IAIndisponivelException(
                    "Pedido interrompido enquanto aguardava o Ollama", e);
        } catch (IllegalStateException e) {
            throw new IAIndisponivelException(e.getMessage(), e);
        }
    }

    private String construirCorpoPedido(List<ChatMessage> mensagens, boolean stream) throws IOException {
        return construirCorpoPedido(mensagens, stream, false);
    }

    private String construirCorpoPedido(List<ChatMessage> mensagens, boolean stream, boolean forcarJson) throws IOException {
        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("model", model);
        raiz.put("stream", stream);
        // Pede JSON nativo ao Ollama (modelos recentes). Fallback: o parser robusto trata texto livre.
        if (forcarJson && !stream) {
            raiz.put("format", "json");
        }

        ArrayNode mensagensNode = raiz.putArray("messages");
        for (ChatMessage mensagem : mensagens) {
            ObjectNode mensagemNode = mensagensNode.addObject();
            mensagemNode.put("role", mensagem.role().name().toLowerCase());
            mensagemNode.put("content", mensagem.content());
        }

        // Parâmetros orientados a IA local
        ObjectNode options = raiz.putObject("options");
        options.put("top_p", topP);
        options.put("repeat_penalty", repeatPenalty);
        options.put("num_ctx", numCtx);
        if (forcarJson) {
            // JSON de questões/flashcards precisa de muito mais tokens; senão corta a meio
            options.put("temperature", Math.min(temperature, 0.15));
            options.put("num_predict", numPredictJson);
        } else {
            options.put("temperature", temperature);
            options.put("num_predict", numPredict);
        }

        return objectMapper.writeValueAsString(raiz);
    }
}
