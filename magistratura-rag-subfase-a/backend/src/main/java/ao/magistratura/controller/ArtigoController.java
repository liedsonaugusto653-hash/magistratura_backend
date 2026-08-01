package ao.magistratura.controller;

import ao.magistratura.dto.biblioteca.ArtigoDetailResponse;
import ao.magistratura.dto.biblioteca.ArtigoResumoResponse;
import ao.magistratura.service.BibliotecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/artigos")
@RequiredArgsConstructor
@Tag(name = "Biblioteca — Artigos", description = "Consulta de artigos legais")
@SecurityRequirement(name = "bearerAuth")
public class ArtigoController {

    private final BibliotecaService bibliotecaService;

    @GetMapping
    @Operation(summary = "Lista artigos (resumo). Filtros: termo, diplomaId. Suporta paginação.")
    public ResponseEntity<Page<ArtigoResumoResponse>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) UUID diplomaId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(bibliotecaService.listarArtigos(termo, diplomaId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe completo de um artigo, incluindo o texto")
    public ResponseEntity<ArtigoDetailResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(bibliotecaService.obterArtigo(id));
    }
}
