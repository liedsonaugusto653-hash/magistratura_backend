package ao.magistratura.controller;

import ao.magistratura.dto.favorito.FavoritoRequest;
import ao.magistratura.dto.favorito.FavoritoResponse;
import ao.magistratura.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
@Tag(name = "Favoritos", description = "Artigos e diplomas marcados pelo estudante")
@SecurityRequirement(name = "bearerAuth")
public class FavoritoController {

    private final FavoritoService favoritoService;

    @GetMapping
    @Operation(summary = "Lista os favoritos do estudante autenticado")
    public ResponseEntity<List<FavoritoResponse>> listar(Authentication auth) {
        return ResponseEntity.ok(favoritoService.listar(auth.getName()));
    }

    @GetMapping("/artigos/{artigoId}/estado")
    @Operation(summary = "Indica se o artigo está nos favoritos do estudante")
    public ResponseEntity<Map<String, Boolean>> estadoArtigo(
            @PathVariable UUID artigoId,
            Authentication auth
    ) {
        boolean favorito = favoritoService.isArtigoFavorito(auth.getName(), artigoId);
        return ResponseEntity.ok(Map.of("favorito", favorito));
    }

    @PostMapping
    @Operation(summary = "Adiciona um artigo ou diploma aos favoritos")
    public ResponseEntity<FavoritoResponse> adicionar(
            @Valid @RequestBody FavoritoRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(201).body(favoritoService.adicionar(auth.getName(), request));
    }

    @DeleteMapping("/artigos/{artigoId}")
    @Operation(summary = "Remove um artigo dos favoritos")
    public ResponseEntity<Void> removerArtigo(@PathVariable UUID artigoId, Authentication auth) {
        favoritoService.removerArtigo(auth.getName(), artigoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um favorito pelo seu identificador")
    public ResponseEntity<Void> remover(@PathVariable UUID id, Authentication auth) {
        favoritoService.remover(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
