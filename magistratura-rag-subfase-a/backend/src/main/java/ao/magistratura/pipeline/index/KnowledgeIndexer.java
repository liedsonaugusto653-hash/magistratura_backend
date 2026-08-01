package ao.magistratura.pipeline.index;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de indexação vetorial / RAG.
 * Implementações futuras: PostgreSQL+pgvector, Qdrant, Milvus, etc.
 */
public interface KnowledgeIndexer {

    void indexArticle(UUID artigoId);

    void removeArticle(UUID artigoId);

    void updateArticle(UUID artigoId);

    List<UUID> searchRelevantChunks(String query, int limite, UUID diplomaId);

    void removeByDocumento(UUID documentoId);
}
