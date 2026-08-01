package ao.magistratura.controller;

import ao.magistratura.dto.flashcard.FlashcardRequest;
import ao.magistratura.dto.flashcard.FlashcardResponse;
import ao.magistratura.dto.flashcard.FlashcardRevisarRequest;
import ao.magistratura.dto.flashcard.FlashcardRevisarResponse;
import ao.magistratura.service.FlashcardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
@Tag(name = "Flashcards", description = "Banco de cartões e progresso individual de estudo")
@SecurityRequirement(name = "bearerAuth")
public class FlashcardController {

    private final FlashcardService flashcardService;


    @PostMapping
    @Operation(summary = "Cria um flashcard manualmente")
    public ResponseEntity<FlashcardResponse> criar(
            @Valid @RequestBody FlashcardRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(201).body(flashcardService.criar(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza um flashcard existente")
    public ResponseEntity<FlashcardResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody FlashcardRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(flashcardService.actualizar(id, request, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Lista flashcards com o progresso do estudante autenticado")
    public ResponseEntity<List<FlashcardResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(flashcardService.listar(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um flashcard com progresso do estudante")
    public ResponseEntity<FlashcardResponse> obter(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(flashcardService.obter(id, authentication.getName()));
    }

    @PostMapping("/{id}/revisar")
    @Operation(summary = "Regista uma revisão (acerto/erro) e atualiza o progresso")
    public ResponseEntity<FlashcardRevisarResponse> revisar(
            @PathVariable UUID id,
            @Valid @RequestBody FlashcardRevisarRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(flashcardService.revisar(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina um flashcard da biblioteca de estudo")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        flashcardService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
