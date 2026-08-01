package ao.magistratura.config;

import ao.magistratura.filter.AuthRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Regista o rate-limit filter sem tocar em SecurityConfig.
 * Ordem alta para correr antes do filtro de autenticação JWT.
 */
@Configuration
public class AuthRateLimitConfig {

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitRegistration(
            AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/api/auth/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        reg.setName("authRateLimitFilter");
        return reg;
    }
}
