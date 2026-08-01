package ao.magistratura.repository;

import ao.magistratura.entity.Questao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestaoRepository extends JpaRepository<Questao, UUID> {
}
