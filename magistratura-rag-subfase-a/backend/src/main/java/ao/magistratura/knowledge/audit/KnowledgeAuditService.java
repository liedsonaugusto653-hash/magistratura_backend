package ao.magistratura.knowledge.audit;

import ao.magistratura.knowledge.model.KnowledgeKind;
import ao.magistratura.pipeline.model.GenerationProvenance;
import ao.magistratura.pipeline.model.KnowledgeChangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Auditoria do Knowledge Domain (logs estruturados por agora).
 * Fase 4 pode persistir numa tabela dedicada knowledge_auditoria.
 */
@Service
public class KnowledgeAuditService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAuditService.class);

    public void registarGeracao(KnowledgeKind kind, KnowledgeChangeSet changes, int quantidade,
                                GenerationProvenance provenance) {
        log.info("[knowledge-audit] kind={} qtd={} novos={} alterados={} pipeline={} provider={} model={} prompt={}",
                kind,
                quantidade,
                changes != null ? changes.getArtigosNovosIds().size() : 0,
                changes != null ? changes.getArtigosAlteradosIds().size() : 0,
                provenance != null ? provenance.pipelineVersion() : null,
                provenance != null ? provenance.aiProvider() : null,
                provenance != null ? provenance.aiModel() : null,
                provenance != null ? provenance.promptVersion() : null);
    }

    public void registarObsolescencia(KnowledgeKind kind, String originKey, int quantidade) {
        log.info("[knowledge-audit] OBSOLETO kind={} origin={} qtd={}", kind, originKey, quantidade);
    }
}
