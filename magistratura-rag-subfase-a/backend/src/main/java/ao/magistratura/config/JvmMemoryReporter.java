package ao.magistratura.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;

/**
 * Escreve no log os limites reais de Heap e Metaspace no arranque.
 * Serve para confirmar se as flags do Dockerfile / JAVA_TOOL_OPTIONS
 * chegaram mesmo ao processo no Render.
 */
@Component
@Order(0)
public class JvmMemoryReporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JvmMemoryReporter.class);

    @Override
    public void run(ApplicationArguments args) {
        Runtime rt = Runtime.getRuntime();
        long heapMaxMb = rt.maxMemory() / (1024 * 1024);
        long heapTotalMb = rt.totalMemory() / (1024 * 1024);
        long heapFreeMb = rt.freeMemory() / (1024 * 1024);

        long metaUsed = -1;
        long metaMax = -1;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            String name = pool.getName();
            if (name != null && name.toLowerCase().contains("metaspace")) {
                MemoryUsage u = pool.getUsage();
                metaUsed = u.getUsed() / (1024 * 1024);
                metaMax = u.getMax() > 0 ? u.getMax() / (1024 * 1024) : -1;
                break;
            }
        }

        log.info("=== JVM MEMORY (confirma flags do container) ===");
        log.info("Heap max={}MB total={}MB free={}MB used≈{}MB",
                heapMaxMb, heapTotalMb, heapFreeMb, heapTotalMb - heapFreeMb);
        log.info("Metaspace used={}MB max={}MB", metaUsed, metaMax);
        log.info("Processors={} | JAVA_TOOL_OPTIONS={}",
                rt.availableProcessors(),
                System.getenv().getOrDefault("JAVA_TOOL_OPTIONS", "<não definido>"));
        log.info("================================================");

        if (metaMax > 0 && metaMax < 180) {
            log.warn("Metaspace max={}MB é BAIXO para este backend. "
                    + "No Render: define JAVA_TOOL_OPTIONS=-XX:MaxMetaspaceSize=256m "
                    + "ou actualiza o Dockerfile e faz Clear Build Cache.", metaMax);
        }
    }
}
