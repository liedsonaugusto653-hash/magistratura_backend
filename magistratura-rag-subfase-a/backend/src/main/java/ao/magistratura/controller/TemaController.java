package ao.magistratura.controller;

import ao.magistratura.dto.biblioteca.TemaResponse;
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
@RequestMapping("/api/temas")
@RequiredArgsConstructor
@Tag(name = "Biblioteca — Temas", description = "Temas jurídicos")
@SecurityRequirement(name = "bearerAuth")
public class TemaController {

    private final BibliotecaService bibliotecaService;

    @GetMapping
    @Operation(summary = "Lista todos os temas")
    public ResponseEntity<List<TemaResponse>> listar() {
        return ResponseEntity.ok(bibliotecaService.listarTemas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um tema")
    public ResponseEntity<TemaResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(bibliotecaService.obterTema(id));
    }
}
