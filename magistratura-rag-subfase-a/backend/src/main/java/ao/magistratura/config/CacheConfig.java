package ao.magistratura.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache local (Caffeine) — RAG e leituras quentes.
 * Tamanhos reduzidos por omissão para free tier (cada entrada pode conter
 * extratos jurídicos longos). Em multi-instância, evoluir para Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String RAG_QUERIES = "ragQueries";
    public static final String ARTIGO_BY_ID = "artigoById";

    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.rag-ttl-seconds:90}") long ragTtlSeconds,
            @Value("${app.cache.rag-max-size:64}") int ragMaxSize
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager(RAG_QUERIES, ARTIGO_BY_ID);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Math.max(30, ragTtlSeconds), TimeUnit.SECONDS)
                .maximumSize(Math.max(16, ragMaxSize))
                .recordStats());
        return manager;
    }
}
