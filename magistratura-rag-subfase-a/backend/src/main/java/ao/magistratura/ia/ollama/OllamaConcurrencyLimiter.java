package ao.magistratura.ia.ollama;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Limita chamadas simultâneas ao Ollama (chat + embeddings).
 * Activo se o chat OU os embeddings usarem Ollama.
 */
@Component
@ConditionalOnExpression(
        "'${app.ia.provider:ollama}'.equalsIgnoreCase('ollama') || '${app.knowledge.embedding-provider:ollama}'.equalsIgnoreCase('ollama')"
)
public class OllamaConcurrencyLimiter {

    private final Semaphore semaphore;
    private final long acquireTimeoutSeconds;

    public OllamaConcurrencyLimiter(
            @Value("${app.ollama.max-concurrent:2}") int maxConcurrent,
            @Value("${app.ollama.acquire-timeout-seconds:120}") long acquireTimeoutSeconds) {
        int permits = Math.max(1, maxConcurrent);
        this.semaphore = new Semaphore(permits, true);
        this.acquireTimeoutSeconds = Math.max(5, acquireTimeoutSeconds);
    }

    public void acquire() throws InterruptedException {
        boolean ok = semaphore.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS);
        if (!ok) {
            throw new IllegalStateException(
                    "Ollama ocupado: limite de concorrência atingido. Tenta novamente em breve.");
        }
    }

    public void release() {
        semaphore.release();
    }
}
