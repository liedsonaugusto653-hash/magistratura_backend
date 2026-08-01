package ao.magistratura.repository;

import ao.magistratura.entity.Diploma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DiplomaRepository extends JpaRepository<Diploma, UUID> {

    Page<Diploma> findByCategoriaId(UUID categoriaId, Pageable pageable);

    @Query("select d from Diploma d where lower(d.titulo) like lower(concat('%', :termo, '%')) " +
           "or lower(d.numero) like lower(concat('%', :termo, '%'))")
    Page<Diploma> pesquisar(String termo, Pageable pageable);
}
