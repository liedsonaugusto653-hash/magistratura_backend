package ao.magistratura.controller;

import ao.magistratura.dto.questao.QuestaoCompletaResponse;
import ao.magistratura.dto.questao.QuestaoRequest;
import ao.magistratura.dto.questao.QuestaoResponse;
import ao.magistratura.dto.questao.ResponderQuestaoRequest;
import ao.magistratura.dto.questao.ResponderQuestaoResponse;
import ao.magistratura.service.QuestaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/questoes")
@RequiredArgsConstructor
@Tag(name = "Questões", description = "Banco de questões e respostas do estudante")
@SecurityRequirement(name = "bearerAuth")
public class QuestaoController {

    private final QuestaoService questaoService;


    @PostMapping
    @Operation(summary = "Cria uma questão manualmente")
    public ResponseEntity<QuestaoResponse> criar(@Valid @RequestBody QuestaoRequest request) {
        return ResponseEntity.status(201).body(questaoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza uma questão existente")
    public ResponseEntity<QuestaoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody QuestaoRequest request
    ) {
        return ResponseEntity.ok(questaoService.actualizar(id, request));
    }

    @GetMapping
    @Operation(summary = "Lista questões (sem revelar a resposta correta). Suporta paginação.")
    public ResponseEntity<Page<QuestaoResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(questaoService.listar(pageable));
    }

    @GetMapping("/{id}/completo")
    @Operation(summary = "Detalhe completo para edição (inclui gabarito)")
    public ResponseEntity<QuestaoCompletaResponse> obterCompleto(@PathVariable UUID id) {
        return ResponseEntity.ok(questaoService.obterCompleta(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma questão (sem revelar a resposta correta)")
    public ResponseEntity<QuestaoResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(questaoService.obter(id));
    }

    @PostMapping("/{id}/responder")
    @Operation(summary = "Regista a resposta do estudante e devolve o resultado + justificativa")
    public ResponseEntity<ResponderQuestaoResponse> responder(
            @PathVariable UUID id,
            @Valid @RequestBody ResponderQuestaoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(questaoService.responder(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina uma questão do banco de questões")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        questaoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
