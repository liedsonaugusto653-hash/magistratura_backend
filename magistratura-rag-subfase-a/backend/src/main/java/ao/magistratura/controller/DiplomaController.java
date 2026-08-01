package ao.magistratura.controller;

import ao.magistratura.dto.biblioteca.DiplomaDetailResponse;
import ao.magistratura.dto.biblioteca.DiplomaRequest;
import ao.magistratura.dto.biblioteca.DiplomaResumoResponse;
import ao.magistratura.service.BibliotecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/diplomas")
@RequiredArgsConstructor
@Tag(name = "Biblioteca — Diplomas", description = "Consulta e criação de diplomas legais")
@SecurityRequirement(name = "bearerAuth")
public class DiplomaController {

    private final BibliotecaService bibliotecaService;

    @PostMapping
    @Operation(summary = "Cria um novo diploma, para associar documentos importados durante o processamento")
    public ResponseEntity<DiplomaDetailResponse> criar(@RequestBody @Valid DiplomaRequest dto) {
        DiplomaDetailResponse criado = bibliotecaService.criarDiploma(dto);
        return ResponseEntity.status(201).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista diplomas (resumo). Filtros: termo, categoriaId. Suporta paginação.")
    public ResponseEntity<Page<DiplomaResumoResponse>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) UUID categoriaId,
            @PageableDefault(size = 200) Pageable pageable
    ) {
        return ResponseEntity.ok(bibliotecaService.listarDiplomas(termo, categoriaId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um diploma, incluindo lista resumida de artigos")
    public ResponseEntity<DiplomaDetailResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(bibliotecaService.obterDiploma(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza os metadados de um diploma")
    public ResponseEntity<DiplomaDetailResponse> actualizar(
            @PathVariable UUID id,
            @RequestBody @Valid DiplomaRequest dto
    ) {
        return ResponseEntity.ok(bibliotecaService.actualizarDiploma(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina um diploma e os artigos associados")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        bibliotecaService.eliminarDiploma(id);
        return ResponseEntity.noContent().build();
    }
}
