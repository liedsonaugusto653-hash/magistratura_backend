package ao.magistratura.repository;

import ao.magistratura.entity.RespostaEstudante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RespostaEstudanteRepository extends JpaRepository<RespostaEstudante, UUID> {
    List<RespostaEstudante> findByUtilizadorIdOrderByDataRespostaDesc(UUID utilizadorId);
    long countByUtilizadorId(UUID utilizadorId);
    long countByUtilizadorIdAndCorretaTrue(UUID utilizadorId);
}
