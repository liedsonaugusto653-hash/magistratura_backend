package ao.magistratura.knowledge.vector;

import java.util.List;
import java.util.UUID;

public interface VectorStore {
    void upsert(VectorRecord record);
    void upsertAll(List<VectorRecord> records);
    void deleteById(UUID id);
    void deleteByDocumentoId(UUID documentoId);
    void deleteByArtigoId(UUID artigoId);
    List<VectorSearchHit> search(VectorSearchRequest request);
}
