package ao.magistratura.repository;

import ao.magistratura.entity.Tema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TemaRepository extends JpaRepository<Tema, UUID> {
}
