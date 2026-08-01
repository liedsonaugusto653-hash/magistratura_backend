package ao.magistratura.config;

import ao.magistratura.filter.IaRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class IaRateLimitConfig {

    @Bean
    public FilterRegistrationBean<IaRateLimitFilter> iaRateLimitFilterRegistration(IaRateLimitFilter filter) {
        FilterRegistrationBean<IaRateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/api/ia/*", "/api/simulados/*");
        reg.setName("iaRateLimitFilter");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return reg;
    }
}
