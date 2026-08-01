package ao.magistratura.repository;

import ao.magistratura.entity.TopicoJuridico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicoJuridicoRepository extends JpaRepository<TopicoJuridico, UUID> {

    Optional<TopicoJuridico> findByCodigo(String codigo);

    List<TopicoJuridico> findByEntidadeIdAndActivoTrueOrderByOrdemAscNomeAsc(UUID entidadeId);

    List<TopicoJuridico> findByParentIdAndActivoTrueOrderByOrdemAscNomeAsc(UUID parentId);

    List<TopicoJuridico> findByActivoTrueAndParentIsNullOrderByOrdemAscNomeAsc();

    @Query("""
            SELECT t FROM TopicoJuridico t
            WHERE t.activo = true
              AND (LOWER(t.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                OR LOWER(t.codigo) LIKE LOWER(CONCAT('%', :termo, '%'))
                OR LOWER(COALESCE(t.descricao, '')) LIKE LOWER(CONCAT('%', :termo, '%')))
            ORDER BY t.ordem ASC, t.nome ASC
            """)
    List<TopicoJuridico> pesquisar(@Param("termo") String termo);
}
