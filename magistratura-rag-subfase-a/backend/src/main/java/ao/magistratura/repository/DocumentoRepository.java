package ao.magistratura.repository;

import ao.magistratura.entity.Documento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {


    @EntityGraph(attributePaths = {"categoria", "diploma"})
    Page<Documento> findAll(Pageable pageable);


    @EntityGraph(attributePaths = {"categoria", "diploma"})
    Optional<Documento> findById(UUID id);


    Optional<Documento> findByHashFicheiro(String hashFicheiro);


    @EntityGraph(attributePaths = {"categoria", "diploma"})
    Page<Documento> findByDiplomaId(UUID diplomaId, Pageable pageable);

    long countByDiplomaId(UUID diplomaId);


    @EntityGraph(attributePaths = {"categoria", "diploma"})
    Page<Documento> findByCategoriaId(UUID categoriaId, Pageable pageable);


    @EntityGraph(attributePaths = {"categoria", "diploma"})
    @Query("""
        select d from Documento d 
        where lower(d.titulo) like lower(concat('%', :termo, '%'))
        or lower(d.fonte) like lower(concat('%', :termo, '%'))
    """)
    Page<Documento> pesquisar(String termo, Pageable pageable);
}