package ao.magistratura.ia;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Estado partilhado da quota/limites upstream (Groq, Gemini, OpenAI, …).
 * Actualizado pelos AIProviders quando recebem HTTP 429 (ou equivalente).
 */
@Component
public class IaQuotaState {

    public record Snapshot(
            boolean rateLimited,
            String provider,
            Integer httpStatus,
            Long retryAfterSeconds,
            Instant limitedAt,
            Instant retryAt,
            String message
    ) {}

    private final AtomicReference<Snapshot> last = new AtomicReference<>(
            new Snapshot(false, null, null, null, null, null, null));

    public void recordRateLimited(String provider, int httpStatus, Long retryAfterSeconds, String message) {
        Instant now = Instant.now();
        Long retry = retryAfterSeconds;
        if (retry == null || retry <= 0) {
            retry = 60L;
        }
        Instant retryAt = now.plusSeconds(retry);
        last.set(new Snapshot(true, provider, httpStatus, retry, now, retryAt, message));
    }

    public void recordSuccess(String provider) {
        Snapshot cur = last.get();
        if (cur != null && cur.rateLimited() && provider != null
                && provider.equalsIgnoreCase(String.valueOf(cur.provider()))) {
            last.set(new Snapshot(false, provider, null, null, null, null, null));
        }
    }

    public Snapshot snapshot() {
        Snapshot s = last.get();
        if (s == null) {
            return new Snapshot(false, null, null, null, null, null, null);
        }
        if (s.rateLimited() && s.retryAt() != null && Instant.now().isAfter(s.retryAt())) {
            // janela de espera terminou — deixa de assinalar como limitado
            Snapshot cleared = new Snapshot(false, s.provider(), s.httpStatus(), null, s.limitedAt(), s.retryAt(), s.message());
            last.compareAndSet(s, cleared);
            return cleared;
        }
        return s;
    }

    public Map<String, Object> asMap() {
        Snapshot s = snapshot();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("upstreamRateLimited", s.rateLimited());
        m.put("upstreamProvider", s.provider());
        m.put("upstreamHttpStatus", s.httpStatus());
        m.put("retryAfterSeconds", s.retryAfterSeconds());
        m.put("limitedAt", s.limitedAt() != null ? s.limitedAt().toString() : null);
        m.put("retryAt", s.retryAt() != null ? s.retryAt().toString() : null);
        m.put("upstreamMessage", s.message());
        return m;
    }

    /** Extrai Retry-After (segundos) de um header HTTP, se existir. */
    public static Long parseRetryAfterSeconds(java.net.http.HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        return headers.firstValue("Retry-After")
                .map(v -> {
                    try {
                        return Long.parseLong(v.trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
    }
}
