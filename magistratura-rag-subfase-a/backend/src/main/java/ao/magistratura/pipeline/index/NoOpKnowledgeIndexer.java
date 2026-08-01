package ao.magistratura.pipeline.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Indexador vazio — ativo só com app.knowledge.indexer=noop.
 * Por omissão usa-se {@link ao.magistratura.knowledge.index.KnowledgeLayerIndexer}.
 */
@Component
@ConditionalOnProperty(name = "app.knowledge.indexer", havingValue = "noop")
public class NoOpKnowledgeIndexer implements KnowledgeIndexer {

    private static final Logger log = LoggerFactory.getLogger(NoOpKnowledgeIndexer.class);

    @Override
    public void indexArticle(UUID artigoId) {
        log.debug("NoOpKnowledgeIndexer.indexArticle({})", artigoId);
    }

    @Override
    public void removeArticle(UUID artigoId) {
        log.debug("NoOpKnowledgeIndexer.removeArticle({})", artigoId);
    }

    @Override
    public void updateArticle(UUID artigoId) {
        log.debug("NoOpKnowledgeIndexer.updateArticle({})", artigoId);
    }

    @Override
    public List<UUID> searchRelevantChunks(String query, int limite, UUID diplomaId) {
        return List.of();
    }

    @Override
    public void removeByDocumento(UUID documentoId) {
        log.debug("NoOpKnowledgeIndexer.removeByDocumento({})", documentoId);
    }
}
