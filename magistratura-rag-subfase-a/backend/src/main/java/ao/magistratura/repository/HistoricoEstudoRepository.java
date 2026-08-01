package ao.magistratura.repository;

import ao.magistratura.entity.HistoricoEstudo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HistoricoEstudoRepository extends JpaRepository<HistoricoEstudo, UUID> {

    List<HistoricoEstudo> findTop10ByUtilizadorIdOrderByDataDesc(UUID utilizadorId);

    @Query("SELECT COALESCE(SUM(h.tempoSegundos), 0) FROM HistoricoEstudo h WHERE h.utilizador.id = :uid")
    long sumTempoSegundosByUtilizadorId(@Param("uid") UUID utilizadorId);

    @Query("SELECT h.data FROM HistoricoEstudo h WHERE h.utilizador.id = :uid ORDER BY h.data DESC")
    List<Instant> findDatasByUtilizadorId(@Param("uid") UUID utilizadorId);
}
