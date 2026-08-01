package ao.magistratura.repository;

import ao.magistratura.entity.MensagemIa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensagemIaRepository extends JpaRepository<MensagemIa, UUID> {

    List<MensagemIa> findByConversaIdOrderByTimestampAsc(UUID conversaId);

    List<MensagemIa> findByConversaIdOrderByTimestampDesc(UUID conversaId, Pageable pageable);
}
