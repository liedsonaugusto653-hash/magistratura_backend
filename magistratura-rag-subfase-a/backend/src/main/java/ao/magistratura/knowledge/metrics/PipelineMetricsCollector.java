package ao.magistratura.knowledge.metrics;

import ao.magistratura.pipeline.model.IncrementalDecision;
import ao.magistratura.pipeline.model.KnowledgeChangeSet;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Pontos de recolha de métricas do pipeline.
 * Não constrói dashboards — apenas persiste agregados por execução.
 */
@Service
@RequiredArgsConstructor
public class PipelineMetricsCollector {

    private final PipelineMetricsRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarExecucao(PipelineContexto ctx, long duracaoTotalMs, int conhecimentoGerado, boolean falhou) {
        IncrementalDecision d = ctx.getDecisaoIncremental();
        KnowledgeChangeSet cs = ctx.getKnowledgeChangeSet();

        int novos = d != null ? d.getArtigosNovos().size() : 0;
        int alterados = d != null ? d.getArtigosAlterados().size() : 0;
        int removidos = d != null ? d.getArtigosRemovidos().size() : 0;
        if (cs != null) {
            novos = Math.max(novos, cs.getArtigosNovosIds().size());
            alterados = Math.max(alterados, cs.getArtigosAlteradosIds().size());
            removidos = Math.max(removidos, cs.getArtigosRemovidosNumeros().size());
        }

        int extraidos = ctx.getArtigosExtraidos() != null ? ctx.getArtigosExtraidos().size() : 0;

        PipelineMetrics m = PipelineMetrics.builder()
                .documentoId(ctx.getDocumento() != null ? ctx.getDocumento().getId() : null)
                .execucaoId(ctx.getExecucaoId())
                .pipelineVersion(PipelineVersion.ATUAL)
                .dataRegisto(Instant.now())
                .documentosProcessados(1)
                .artigosExtraidos(extraidos)
                .artigosNovos(novos)
                .artigosAlterados(alterados)
                .artigosRemovidos(removidos)
                .conhecimentoGerado(conhecimentoGerado)
                .falhas(falhou ? 1 : 0)
                .duracaoTotalMs(duracaoTotalMs)
                .detalheEtapas(d != null ? d.getMotivo() : null)
                .build();
        repository.save(m);
    }
}
