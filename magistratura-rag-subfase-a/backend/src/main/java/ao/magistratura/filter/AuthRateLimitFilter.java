package ao.magistratura.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting simples in-memory para endpoints de autenticação.
 * Protege login, registo e recuperação de password contra brute-force / spam.
 *
 * Limites (por IP, janela de 1 minuto):
 *   /api/auth/login              → 10 pedidos
 *   /api/auth/registo            → 5 pedidos
 *   /api/auth/recuperar-password → 5 pedidos
 *
 * Para multi-instância, substituir por Bucket4j + Redis sem alterar controllers.
 * NÃO altera SecurityConfig — regista-se via FilterRegistrationBean ou @Component
 * no filter chain (ver AuthRateLimitConfig).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !(path.endsWith("/api/auth/login")
                || path.endsWith("/api/auth/registo")
                || path.endsWith("/api/auth/recuperar-password"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int limit = path.endsWith("/login") ? 10 : 5;
        String key = clientKey(request) + ":" + path;

        Window w = windows.compute(key, (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (w.count.get() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", 429,
                    "error", "Demasiados pedidos. Tenta novamente dentro de um minuto.",
                    "timestamp", Instant.now().toString()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
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
