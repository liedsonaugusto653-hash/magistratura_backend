package ao.magistratura.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limit para endpoints de geração de IA (tutor, simulados, ferramentas).
 * Janela deslizante por utilizador autenticado (header Authorization) ou IP.
 */
@Component
public class IaRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int limitPerMinute;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public IaRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.ia-rate-limit.enabled:true}") boolean enabled,
            @Value("${app.ia-rate-limit.requests-per-minute:20}") int limitPerMinute) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.limitPerMinute = Math.max(1, limitPerMinute);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getLimitPerMinute() {
        return limitPerMinute;
    }

    /**
     * Snapshot do uso local na janela actual (não incrementa contador).
     */
    public Map<String, Object> snapshot(HttpServletRequest request) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("localLimitEnabled", enabled);
        m.put("localLimitPerMinute", limitPerMinute);
        if (!enabled || request == null) {
            m.put("localUsed", 0);
            m.put("localRemaining", limitPerMinute);
            m.put("localResetAt", null);
            return m;
        }
        String key = clientKey(request);
        Window w = windows.get(key);
        long now = Instant.now().toEpochMilli();
        if (w == null || now - w.windowStart > WINDOW_MS) {
            m.put("localUsed", 0);
            m.put("localRemaining", limitPerMinute);
            m.put("localResetAt", Instant.ofEpochMilli(now + WINDOW_MS).toString());
            return m;
        }
        int used = w.count.get();
        long resetAt = w.windowStart + WINDOW_MS;
        m.put("localUsed", used);
        m.put("localRemaining", Math.max(0, limitPerMinute - used));
        m.put("localResetAt", Instant.ofEpochMilli(resetAt).toString());
        return m;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !(path.startsWith("/api/ia/")
                || path.equals("/api/simulados/gerar")
                || path.contains("/api/ia/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = clientKey(request);
        Window w = windows.compute(key, (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (w.count.get() > limitPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            long resetSec = Math.max(1, (w.windowStart + WINDOW_MS - Instant.now().toEpochMilli()) / 1000);
            response.setHeader("Retry-After", String.valueOf(resetSec));
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", 429,
                    "error", "Limite de pedidos de IA atingido. Aguarda cerca de um minuto e tenta de novo.",
                    "retryAfterSeconds", resetSec,
                    "timestamp", Instant.now().toString()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    static String clientKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ") && auth.length() > 20) {
            return "u:" + auth.substring(auth.length() - 16);
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return "ip:" + xff.split(",")[0].trim();
        }
        return "ip:" + (request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown");
    }

    private static final class Window {
        final long windowStart;
        final AtomicInteger count;

        Window(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
