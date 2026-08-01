package ao.magistratura.pipeline.stage;

import ao.magistratura.entity.Artigo;
import ao.magistratura.knowledge.generator.KnowledgeGenerator;
import ao.magistratura.knowledge.origin.KnowledgeOriginService;
import ao.magistratura.pipeline.model.KnowledgeChangeSet;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.repository.ArtigoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ponte Pipeline → Knowledge Domain.
 * Não escreve em tabelas de negócio: delega em {@link KnowledgeGenerator}.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeGeneratorStage implements PipelineStage {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGeneratorStage.class);

    private final ArtigoRepository artigoRepository;
    private final KnowledgeOriginService knowledgeOriginService;
    private final KnowledgeGenerator knowledgeGenerator;

    @Value("${app.pipeline.conhecimento-automatico:false}")
    private boolean ativo;

    @Override
    public PipelineEtapa etapa() {
        return PipelineEtapa.GERANDO_CONHECIMENTO;
    }

    @Override
    public void executar(PipelineContexto ctx) {
        ctx.setConhecimentoAutomaticoAtivo(ativo);

        // Garante origens lógicas estáveis para todos os artigos deste documento
        List<Artigo> persistidos = artigoRepository.findByDocumentoIdOrderByOrdemAsc(ctx.getDocumento().getId());
        for (Artigo a : persistidos) {
            knowledgeOriginService.upsertForArtigo(a);
        }

        KnowledgeChangeSet changes = construirChangeSet(ctx, persistidos);
        ctx.setKnowledgeChangeSet(changes);

        if (!ativo) {
            log.info("Knowledge Domain em modo passivo (flag off). origins atualizadas={}; changeSet novos={} alterados={}",
                    persistidos.size(),
                    changes.getArtigosNovosIds().size(),
                    changes.getArtigosAlteradosIds().size());
            return;
        }

        int gerados = knowledgeGenerator.generateIncremental(changes);
        log.info("KnowledgeGenerator.generateIncremental → {} artefactos", gerados);
    }

    private KnowledgeChangeSet construirChangeSet(PipelineContexto ctx, List<Artigo> persistidos) {
        KnowledgeChangeSet cs = new KnowledgeChangeSet();
        cs.setDocumentoId(ctx.getDocumento().getId());
        if (ctx.getDiploma() != null) {
            cs.setDiplomaId(ctx.getDiploma().getId());
        }

        var decisao = ctx.getDecisaoIncremental();
        if (decisao == null
                || (decisao.getArtigosNovos().isEmpty() && decisao.getArtigosAlterados().isEmpty()
                && decisao.getArtigosRemovidos().isEmpty()
                && decisao.getTipo() != null
                && decisao.getTipo().name().equals("NOVO"))) {
            for (Artigo a : persistidos) {
                cs.getArtigosNovosIds().add(a.getId());
            }
            return cs;
        }

        if (decisao.getArtigosNovos().isEmpty() && decisao.getArtigosAlterados().isEmpty()
                && !persistidos.isEmpty()
                && decisao.getTipo() != null
                && decisao.getTipo().name().equals("NOVO")) {
            for (Artigo a : persistidos) {
                cs.getArtigosNovosIds().add(a.getId());
            }
        }

        for (Artigo a : persistidos) {
            if (decisao.getArtigosNovos().contains(a.getNumero())) {
                cs.getArtigosNovosIds().add(a.getId());
            } else if (decisao.getArtigosAlterados().contains(a.getNumero())) {
                cs.getArtigosAlteradosIds().add(a.getId());
            }
        }
        cs.getArtigosRemovidosNumeros().addAll(decisao.getArtigosRemovidos());
        return cs;
    }
}
