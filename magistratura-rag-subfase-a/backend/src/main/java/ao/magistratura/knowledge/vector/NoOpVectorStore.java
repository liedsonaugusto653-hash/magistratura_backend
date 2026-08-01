package ao.magistratura.knowledge.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.knowledge.vector-store", havingValue = "noop", matchIfMissing = true)
public class NoOpVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(NoOpVectorStore.class);

    @Override
    public void upsert(VectorRecord record) {
        log.debug("NoOpVectorStore.upsert id={}", record != null ? record.id() : null);
    }

    @Override
    public void upsertAll(List<VectorRecord> records) {
        log.debug("NoOpVectorStore.upsertAll n={}", records != null ? records.size() : 0);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("NoOpVectorStore.deleteById {}", id);
    }

    @Override
    public void deleteByDocumentoId(UUID documentoId) {
        log.debug("NoOpVectorStore.deleteByDocumentoId {}", documentoId);
    }

    @Override
    public void deleteByArtigoId(UUID artigoId) {
        log.debug("NoOpVectorStore.deleteByArtigoId {}", artigoId);
    }

    @Override
    public List<VectorSearchHit> search(VectorSearchRequest request) {
        return List.of();
    }
}
