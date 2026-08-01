package ao.magistratura.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Falha o arranque em produção se JWT_SECRET ainda for o valor de fallback inseguro.
 * Não altera SecurityConfig nem AuthService.
 */
@Component
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    private static final String INSECURE_DEFAULT =
            "change-this-secret-in-production-please-minimum-256-bits-long";

    private final Environment environment;
    private final String jwtSecret;

    public JwtSecretValidator(
            Environment environment,
            @Value("${app.jwt.secret}") String jwtSecret) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void validate() {
        boolean isProd = false;
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                isProd = true;
                break;
            }
        }

        if (!isProd) {
            if (INSECURE_DEFAULT.equals(jwtSecret)) {
                log.warn("JWT_SECRET está com o valor de fallback. Aceitável em dev; obrigatório alterar em prod.");
            }
            return;
        }

        if (jwtSecret == null || jwtSecret.isBlank() || INSECURE_DEFAULT.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "FATAL: JWT_SECRET inseguro ou em falta com profile 'prod'. "
                            + "Defina JWT_SECRET com pelo menos 32 caracteres aleatórios antes de arrancar.");
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "FATAL: JWT_SECRET demasiado curto em produção (mínimo 32 caracteres).");
        }

        log.info("JWT_SECRET validado para ambiente de produção.");
    }
}
