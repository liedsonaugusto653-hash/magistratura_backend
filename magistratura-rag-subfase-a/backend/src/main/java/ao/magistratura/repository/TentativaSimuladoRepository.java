package ao.magistratura.repository;

import ao.magistratura.entity.TentativaSimulado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TentativaSimuladoRepository extends JpaRepository<TentativaSimulado, UUID> {
    List<TentativaSimulado> findByUtilizadorIdOrderByDataInicioDesc(UUID utilizadorId);
}
