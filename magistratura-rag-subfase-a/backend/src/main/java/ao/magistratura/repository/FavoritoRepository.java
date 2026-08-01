package ao.magistratura.repository;

import ao.magistratura.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {

    List<Favorito> findByUtilizadorIdOrderByDataCriacaoDesc(UUID utilizadorId);

    Optional<Favorito> findByUtilizadorIdAndArtigoId(UUID utilizadorId, UUID artigoId);

    Optional<Favorito> findByUtilizadorIdAndDiplomaId(UUID utilizadorId, UUID diplomaId);

    boolean existsByUtilizadorIdAndArtigoId(UUID utilizadorId, UUID artigoId);

    void deleteByUtilizadorIdAndArtigoId(UUID utilizadorId, UUID artigoId);

    void deleteByUtilizadorIdAndDiplomaId(UUID utilizadorId, UUID diplomaId);
}
