package ao.magistratura.controller;

import ao.magistratura.dto.simulado.*;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.service.SimuladoService;
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
@RequestMapping("/api/simulados")
@RequiredArgsConstructor
@Tag(name = "Simulados", description = "Simulados de concurso e tentativas do estudante")
@SecurityRequirement(name = "bearerAuth")
public class SimuladoController {

    private final SimuladoService simuladoService;


    @PostMapping("/gerar")
    @Operation(summary = "Geração de simulados via IA — DESACTIVADA (preferir questões e flashcards)")
    public ResponseEntity<GerarSimuladoResponse> gerar(@Valid @RequestBody GerarSimuladoRequest request) {
        throw new RegraNegocioException(
                "A geração de simulados por IA está desactivada. "
                        + "Usa Questões e Flashcards para praticar com a legislação da biblioteca.");
    }

    @GetMapping
    @Operation(summary = "Lista simulados disponíveis")
    public ResponseEntity<List<SimuladoResumoResponse>> listar() {
        return ResponseEntity.ok(simuladoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um simulado (questões sem resposta correta)")
    public ResponseEntity<SimuladoDetailResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(simuladoService.obter(id));
    }

    @PostMapping("/{id}/iniciar")
    @Operation(summary = "Inicia uma tentativa de simulado para o estudante autenticado")
    public ResponseEntity<IniciarSimuladoResponse> iniciar(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(simuladoService.iniciar(id, authentication.getName()));
    }

    @PostMapping("/tentativas/{tentativaId}/responder")
    @Operation(summary = "Regista a resposta a uma questão dentro de uma tentativa")
    public ResponseEntity<Void> responder(
            @PathVariable UUID tentativaId,
            @Valid @RequestBody ResponderSimuladoRequest request,
            Authentication authentication
    ) {
        simuladoService.responder(tentativaId, request, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tentativas/{tentativaId}/finalizar")
    @Operation(summary = "Finaliza a tentativa e calcula a pontuação")
    public ResponseEntity<FinalizarSimuladoResponse> finalizar(
            @PathVariable UUID tentativaId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(simuladoService.finalizar(tentativaId, authentication.getName()));
    }

    @GetMapping("/historico")
    @Operation(summary = "Histórico de tentativas concluídas do estudante autenticado")
    public ResponseEntity<List<FinalizarSimuladoResponse>> historico(Authentication authentication) {
        return ResponseEntity.ok(simuladoService.historico(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina um simulado e as suas associações a questões")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        simuladoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
