package ao.magistratura.repository;

import ao.magistratura.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
}
