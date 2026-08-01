package ao.magistratura.repository;

import ao.magistratura.entity.DocumentoEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentoEmbeddingRepository extends JpaRepository<DocumentoEmbedding, UUID> {

    List<DocumentoEmbedding> findByArtigoId(UUID artigoId);

    List<DocumentoEmbedding> findByResumoId(UUID resumoId);

    /**
     * Apaga todos os embeddings cujo artigo pertence ao documento indicado.
     * Necessário antes de {@code artigoRepository.deleteByDocumentoId(...)},
     * porque documento_embeddings.artigo_id não tem ON DELETE CASCADE.
     */
    @Modifying
    @Query("delete from DocumentoEmbedding de where de.artigo.id in " +
           "(select a.id from Artigo a where a.documento.id = :documentoId)")
    void deleteByArtigoDocumentoId(@Param("documentoId") UUID documentoId);
}