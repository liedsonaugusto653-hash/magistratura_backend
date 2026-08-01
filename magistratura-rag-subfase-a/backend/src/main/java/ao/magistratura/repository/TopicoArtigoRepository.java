package ao.magistratura.repository;

import ao.magistratura.entity.TopicoArtigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicoArtigoRepository extends JpaRepository<TopicoArtigo, UUID> {

    Optional<TopicoArtigo> findByTopicoIdAndArtigoId(UUID topicoId, UUID artigoId);

    @Query("""
            SELECT ta FROM TopicoArtigo ta
            JOIN FETCH ta.artigo a
            LEFT JOIN FETCH a.diploma
            WHERE ta.topico.id = :topicoId
            ORDER BY ta.relevancia DESC
            """)
    List<TopicoArtigo> findByTopicoIdComArtigo(@Param("topicoId") UUID topicoId);

    @Query("""
            SELECT ta FROM TopicoArtigo ta
            JOIN FETCH ta.topico t
            WHERE ta.artigo.id = :artigoId
            ORDER BY ta.relevancia DESC
            """)
    List<TopicoArtigo> findByArtigoIdComTopico(@Param("artigoId") UUID artigoId);

    void deleteByTopicoIdAndArtigoId(UUID topicoId, UUID artigoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TopicoArtigo ta WHERE ta.artigo.id IN (SELECT a.id FROM Artigo a WHERE a.documento.id = :documentoId)")
    int deleteByDocumentoId(@Param("documentoId") UUID documentoId);
}
