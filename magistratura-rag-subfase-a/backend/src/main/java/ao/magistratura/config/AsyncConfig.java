package ao.magistratura.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pool assíncrono do pipeline de documentos.
 * <p>
 * No free tier (512 MB) só pode correr 1 job pesado (OCR/PDF) de cada vez —
 * vários em paralelo disparam OOM. Em máquinas maiores sobrescrever
 * {@code app.async.pipeline-core-size} / {@code app.async.pipeline-max-size}.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${app.async.pipeline-core-size:1}")
    private int coreSize;

    @Value("${app.async.pipeline-max-size:1}")
    private int maxSize;

    @Value("${app.async.pipeline-queue-capacity:15}")
    private int queueCapacity;

    @Bean(name = "pipelineExecutor")
    public Executor pipelineExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(Math.max(1, coreSize));
        ex.setMaxPoolSize(Math.max(1, maxSize));
        ex.setQueueCapacity(Math.max(5, queueCapacity));
        ex.setThreadNamePrefix("pipeline-");
        // CallerRuns: se a fila estiver cheia, o pedido HTTP espera em vez de
        // criar mais threads e estoirar a RAM.
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(120);
        ex.initialize();
        log.info("pipelineExecutor: core={}, max={}, queue={}", coreSize, maxSize, queueCapacity);
        return ex;
    }

    @Override
    public Executor getAsyncExecutor() {
        return pipelineExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            if (ex instanceof OutOfMemoryError || (ex.getCause() instanceof OutOfMemoryError)) {
                log.error("OOM em @Async {}.{} — o job foi abortado para proteger o processo",
                        method.getDeclaringClass().getSimpleName(), method.getName(), ex);
            } else {
                log.error("Erro @Async {}.{}: {}", method.getDeclaringClass().getSimpleName(),
                        method.getName(), ex.getMessage(), ex);
            }
        };
    }
}
