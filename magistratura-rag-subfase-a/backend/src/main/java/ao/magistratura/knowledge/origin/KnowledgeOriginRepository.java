package ao.magistratura.knowledge.origin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeOriginRepository extends JpaRepository<KnowledgeOrigin, UUID> {

    Optional<KnowledgeOrigin> findByOriginKey(String originKey);

    List<KnowledgeOrigin> findByDocumentoId(UUID documentoId);
}
