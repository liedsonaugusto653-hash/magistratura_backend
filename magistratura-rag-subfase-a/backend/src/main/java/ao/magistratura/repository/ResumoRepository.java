package ao.magistratura.repository;

import ao.magistratura.entity.Resumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResumoRepository extends JpaRepository<Resumo, UUID> {
}
