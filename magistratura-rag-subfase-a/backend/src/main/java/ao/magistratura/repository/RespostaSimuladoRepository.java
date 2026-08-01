package ao.magistratura.repository;

import ao.magistratura.entity.RespostaSimulado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RespostaSimuladoRepository extends JpaRepository<RespostaSimulado, UUID> {
    List<RespostaSimulado> findByTentativaId(UUID tentativaId);
    Optional<RespostaSimulado> findByTentativaIdAndQuestaoId(UUID tentativaId, UUID questaoId);
}
