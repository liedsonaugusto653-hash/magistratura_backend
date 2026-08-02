package ao.magistratura.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check leve para o Render (e load balancers).
 * <p>
 * Público (sem JWT). Não toca na base de dados nem em serviços pesados —
 * responde em microssegundos e reporta uso de heap para diagnóstico.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "magistratura-backend");

        try {
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = mem.getHeapMemoryUsage();
            Map<String, Object> memory = new LinkedHashMap<>();
            memory.put("heapUsedMb", heap.getUsed() / (1024 * 1024));
            memory.put("heapCommittedMb", heap.getCommitted() / (1024 * 1024));
            memory.put("heapMaxMb", heap.getMax() > 0 ? heap.getMax() / (1024 * 1024) : -1);
            body.put("memory", memory);
        } catch (Exception ignored) {
            // health nunca deve falhar por causa de métricas
        }

        return ResponseEntity.ok(body);
    }
}
