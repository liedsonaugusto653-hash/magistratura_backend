package ao.magistratura.repository;

import ao.magistratura.entity.FlashcardProgresso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardProgressoRepository extends JpaRepository<FlashcardProgresso, UUID> {

    Optional<FlashcardProgresso> findByUtilizadorIdAndFlashcardId(UUID utilizadorId, UUID flashcardId);

    List<FlashcardProgresso> findByUtilizadorId(UUID utilizadorId);

    void deleteByFlashcardId(UUID flashcardId);
}
