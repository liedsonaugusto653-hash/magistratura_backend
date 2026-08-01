package ao.magistratura.repository;

import ao.magistratura.entity.RelacaoJuridica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RelacaoJuridicaRepository extends JpaRepository<RelacaoJuridica, UUID> {

    @Query("""
            SELECT r FROM RelacaoJuridica r
            JOIN FETCH r.destino
            WHERE r.origem.id = :topicoId
            ORDER BY r.peso DESC
            """)
    List<RelacaoJuridica> findByOrigemId(@Param("topicoId") UUID topicoId);

    @Query("""
            SELECT r FROM RelacaoJuridica r
            JOIN FETCH r.origem
            WHERE r.destino.id = :topicoId
            ORDER BY r.peso DESC
            """)
    List<RelacaoJuridica> findByDestinoId(@Param("topicoId") UUID topicoId);

    @Query("""
            SELECT r FROM RelacaoJuridica r
            JOIN FETCH r.destino
            WHERE r.origem.id = :topicoId
              AND UPPER(r.tipoRelacao) = UPPER(:tipo)
            ORDER BY r.peso DESC
            """)
    List<RelacaoJuridica> findByOrigemIdAndTipo(
            @Param("topicoId") UUID topicoId,
            @Param("tipo") String tipo);
}
