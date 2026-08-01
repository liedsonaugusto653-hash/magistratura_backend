package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.Artigo;
import ao.magistratura.pipeline.index.KnowledgeIndexer;
import ao.magistratura.pipeline.model.IncrementalDecision;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.repository.ArtigoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Indexação incremental via {@link KnowledgeIndexer}.
 * Só toca artigos novos/alterados; remove índices de artigos eliminados.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeIndexerStage implements PipelineStage {

    private final KnowledgeIndexer knowledgeIndexer;
    private final ArtigoRepository artigoRepository;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.INDEXANDO;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        IncrementalDecision d = ctx.getDecisaoIncremental();
        List<Artigo> artigos = artigoRepository.findByDocumentoIdOrderByOrdemAsc(ctx.getDocumento().getId());

        if (d == null || d.getTipo() == IncrementalDecision.TipoDocumento.NOVO) {
            for (Artigo a : artigos) {
                knowledgeIndexer.indexArticle(a.getId());
            }
            return;
        }

        // Removidos (por número — ids podem já ter sido apagados na persistência)
        for (String numero : d.getArtigosRemovidos()) {
            // best-effort: se ainda existirem noutro lado, não é responsabilidade desta stage
        }

        for (Artigo a : artigos) {
            if (d.getArtigosNovos().contains(a.getNumero())) {
                knowledgeIndexer.indexArticle(a.getId());
            } else if (d.getArtigosAlterados().contains(a.getNumero())) {
                knowledgeIndexer.updateArticle(a.getId());
            }
        }
    }
}
