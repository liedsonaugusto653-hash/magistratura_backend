package ao.magistratura.knowledge.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import ao.magistratura.ia.ollama.OllamaConcurrencyLimiter;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.knowledge.embedding-provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingProvider.class);
    private static final int CACHE_MAX = 2048;

    private final String baseUrl;
    private final String model;
    private final int dimensions;
    private final boolean failSoft;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();
    private final OllamaConcurrencyLimiter concurrencyLimiter;

    public OllamaEmbeddingProvider(
            @Value("${app.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.knowledge.embedding-model:nomic-embed-text}") String model,
            @Value("${app.knowledge.embedding-dimensions:768}") int dimensions,
            @Value("${app.knowledge.embedding-fail-soft:true}") boolean failSoft,
            OllamaConcurrencyLimiter concurrencyLimiter) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.dimensions = dimensions;
        this.failSoft = failSoft;
        this.concurrencyLimiter = concurrencyLimiter;
    }

    @Override
    public String nome() {
        return "Ollama";
    }

    @Override
    public String modelo() {
        return model;
    }

    @Override
    public int dimensoes() {
        return dimensions;
    }

    @Override
    public float[] embed(String texto) {
        String key = model + ":" + sha256(texto != null ? texto : "");
        float[] cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            concurrencyLimiter.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (failSoft) {
                return new float[dimensions];
            }
            throw new IllegalStateException("Embedding interrompido", ie);
        } catch (IllegalStateException ise) {
            if (failSoft) {
                return new float[dimensions];
            }
            throw ise;
        }
        try {
            String body = mapper.createObjectNode()
                    .put("model", model)
                    .put("prompt", texto != null ? texto : "")
                    .toString();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                throw new IllegalStateException("Ollama embeddings HTTP " + res.statusCode());
            }
            JsonNode arr = mapper.readTree(res.body()).get("embedding");
            if (arr == null || !arr.isArray()) {
                throw new IllegalStateException("Resposta Ollama sem campo embedding");
            }
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                v[i] = (float) arr.get(i).asDouble();
            }
            if (cache.size() < CACHE_MAX) {
                cache.put(key, v);
            }
            return v;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return handleFailure("interrompido", e);
        } catch (Exception e) {
            return handleFailure(e.getMessage(), e);
        } finally {
            concurrencyLimiter.release();
        }
    }

    public void invalidateCache() {
        cache.clear();
    }

    private float[] handleFailure(String msg, Exception e) {
        log.warn("Embedding Ollama indisponível ({}). failSoft={}", msg, failSoft);
        if (failSoft) {
            // Vetor zero: ranking lexical continua a funcionar; vector search ignora/baixa score
            return new float[dimensions];
        }
        throw new RuntimeException("Falha embedding: " + msg, e);
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
