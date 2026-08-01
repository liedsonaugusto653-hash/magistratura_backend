package ao.magistratura.repository;

import ao.magistratura.entity.Estatistica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstatisticaRepository extends JpaRepository<Estatistica, UUID> {
    Optional<Estatistica> findByUtilizadorId(UUID utilizadorId);
}
