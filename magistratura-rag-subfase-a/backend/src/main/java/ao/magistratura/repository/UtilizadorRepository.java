package ao.magistratura.repository;

import ao.magistratura.entity.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    Optional<Utilizador> findByEmail(String email);
    boolean existsByEmail(String email);
}
