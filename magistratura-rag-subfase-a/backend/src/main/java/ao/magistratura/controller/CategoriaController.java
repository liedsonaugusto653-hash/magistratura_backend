package ao.magistratura.controller;

import ao.magistratura.dto.biblioteca.CategoriaResponse;
import ao.magistratura.service.BibliotecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@Tag(name = "Biblioteca — Categorias", description = "Categorias jurídicas")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    private final BibliotecaService bibliotecaService;

    @GetMapping
    @Operation(summary = "Lista todas as categorias")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(bibliotecaService.listarCategorias());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma categoria")
    public ResponseEntity<CategoriaResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(bibliotecaService.obterCategoria(id));
    }
}
