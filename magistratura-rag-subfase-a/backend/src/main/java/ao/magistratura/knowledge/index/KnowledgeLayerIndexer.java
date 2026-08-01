package ao.magistratura.knowledge.index;

import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.api.KnowledgeService;
import ao.magistratura.pipeline.index.KnowledgeIndexer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementação real de {@link KnowledgeIndexer} usada pelo pipeline.
 * Substitui o NoOp quando app.knowledge.indexer=knowledge (default).
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.knowledge.indexer", havingValue = "knowledge", matchIfMissing = true)
@RequiredArgsConstructor
public class KnowledgeLayerIndexer implements KnowledgeIndexer {

    private final KnowledgeService knowledgeService;
    private final IndexingService indexingService;

    @Override
    public void indexArticle(UUID artigoId) {
        knowledgeService.indexArtigo(artigoId);
    }

    @Override
    public void removeArticle(UUID artigoId) {
        indexingService.removeByArtigo(artigoId);
    }

    @Override
    public void updateArticle(UUID artigoId) {
        knowledgeService.indexArtigo(artigoId);
    }

    @Override
    public List<UUID> searchRelevantChunks(String query, int limite, UUID diplomaId) {
        KnowledgeResult r = knowledgeService.search(new KnowledgeQuery(
                query, diplomaId, null, null, KnowledgeContentKind.LEGISLACAO, limite, true));
        return r.passagens().stream()
                .map(KnowledgePassage::artigoId)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void removeByDocumento(UUID documentoId) {
        knowledgeService.removeByDocumento(documentoId);
    }
}
