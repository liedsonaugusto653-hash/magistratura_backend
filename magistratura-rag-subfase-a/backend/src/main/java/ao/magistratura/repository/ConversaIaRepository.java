package ao.magistratura.repository;

import ao.magistratura.entity.ConversaIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversaIaRepository extends JpaRepository<ConversaIa, UUID> {
    List<ConversaIa> findByUtilizadorIdOrderByDataCriacaoDesc(UUID utilizadorId);
}
