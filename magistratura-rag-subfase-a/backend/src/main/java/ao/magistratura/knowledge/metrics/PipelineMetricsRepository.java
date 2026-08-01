package ao.magistratura.knowledge.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineMetricsRepository extends JpaRepository<PipelineMetrics, UUID> {

    List<PipelineMetrics> findByDocumentoIdOrderByDataRegistoDesc(UUID documentoId);
}
