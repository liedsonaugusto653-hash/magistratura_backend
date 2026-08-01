package ao.magistratura.repository;

import ao.magistratura.entity.Artigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtigoRepository extends JpaRepository<Artigo, UUID> {

    List<Artigo> findByDiplomaIdOrderByOrdemAsc(UUID diplomaId);

    List<Artigo> findByDocumentoIdOrderByOrdemAsc(UUID documentoId);

    long countByDocumentoId(UUID documentoId);

    void deleteByDocumentoId(UUID documentoId);

    void deleteByDiplomaId(UUID diplomaId);

    long countByDiplomaId(UUID diplomaId);

    Optional<Artigo> findFirstByDiplomaIdAndOrdemLessThanOrderByOrdemDesc(UUID diplomaId, Integer ordem);

    Optional<Artigo> findFirstByDiplomaIdAndOrdemGreaterThanOrderByOrdemAsc(UUID diplomaId, Integer ordem);

    @Query("select a from Artigo a where lower(a.texto) like lower(concat('%', :termo, '%')) " +
           "or lower(a.titulo) like lower(concat('%', :termo, '%')) " +
           "or lower(a.numero) like lower(concat('%', :termo, '%')) " +
           "or lower(a.capitulo) like lower(concat('%', :termo, '%')) " +
           "or lower(a.seccao) like lower(concat('%', :termo, '%'))")
    Page<Artigo> pesquisar(@Param("termo") String termo, Pageable pageable);

    /**
     * Número flexível: "1", "1.", "1º", "Art. 1" contidos no campo numero.
     */
    @Query("select a from Artigo a join fetch a.diploma d where " +
           "lower(a.numero) = lower(:numero) or lower(a.numero) like lower(concat(:numero, '%')) " +
           "or lower(a.numero) like lower(concat('% ', :numero)) " +
           "or lower(a.numero) like lower(concat('%', :numero, 'º%')) " +
           "or lower(a.numero) like lower(concat('%', :numero, '.%'))")
    List<Artigo> buscarPorNumeroFlexivel(@Param("numero") String numero);

    @Query("select a from Artigo a join fetch a.diploma d where d.id = :diplomaId and (" +
           "lower(a.numero) = lower(:numero) or lower(a.numero) like lower(concat(:numero, '%')) " +
           "or lower(a.numero) like lower(concat('%', :numero, 'º%')) " +
           "or lower(a.numero) like lower(concat('%', :numero, '.%')))")
    List<Artigo> buscarPorNumeroEDiplomaId(@Param("numero") String numero, @Param("diplomaId") UUID diplomaId);

    @Query("select a from Artigo a join fetch a.diploma d where lower(d.titulo) like lower(concat('%', :diplomaTermo, '%')) " +
           "and (lower(a.numero) = lower(:numero) or lower(a.numero) like lower(concat(:numero, '%')) " +
           "or lower(a.numero) like lower(concat('%', :numero, 'º%')) " +
           "or lower(a.numero) like lower(concat('%', :numero, '.%')))")
    List<Artigo> buscarPorNumeroEDiplomaTermo(@Param("numero") String numero, @Param("diplomaTermo") String diplomaTermo);

    @Query("select a from Artigo a join fetch a.diploma d where d.id = :diplomaId and (" +
           "lower(a.texto) like lower(concat('%', :termo, '%')) " +
           "or lower(a.titulo) like lower(concat('%', :termo, '%')) " +
           "or lower(a.numero) like lower(concat('%', :termo, '%')))")
    Page<Artigo> pesquisarNoDiploma(@Param("diplomaId") UUID diplomaId, @Param("termo") String termo, Pageable pageable);
}
