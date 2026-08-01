package ao.magistratura.repository;

import ao.magistratura.entity.Simulado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SimuladoRepository extends JpaRepository<Simulado, UUID> {
}
