package ao.magistratura.controller;

import ao.magistratura.dto.ia.ChatRequest;
import ao.magistratura.dto.ia.ConversaDetailResponse;
import ao.magistratura.dto.ia.ConversaSummaryResponse;
import ao.magistratura.dto.ia.CriarConversaRequest;
import ao.magistratura.dto.ia.ExplicarArtigoRequest;
import ao.magistratura.dto.ia.ExplicarArtigoResponse;
import ao.magistratura.dto.ia.GerarFlashcardsRequest;
import ao.magistratura.dto.ia.GerarFlashcardsResponse;
import ao.magistratura.dto.ia.GerarQuestoesRequest;
import ao.magistratura.dto.ia.GerarQuestoesResponse;
import ao.magistratura.dto.ia.MensagemResponse;
import ao.magistratura.dto.ia.ResumoIARequest;
import ao.magistratura.dto.ia.ResumoIAResponse;
import ao.magistratura.service.TutorService;
import ao.magistratura.ia.IaQuotaState;
import ao.magistratura.filter.IaRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
@Tag(name = "Tutor IA", description = "Chat, conversas e geração de conteúdo de estudo assistidos por IA")
public class TutorController {

    private final TutorService tutorService;
    private final IaQuotaState iaQuotaState;
    private final IaRateLimitFilter iaRateLimitFilter;

    // ------------------------------------------------------------------
    // Conversas
    // ------------------------------------------------------------------

    @GetMapping("/conversas")
    @Operation(summary = "Lista as conversas do estudante autenticado, mais recentes primeiro")
    public ResponseEntity<java.util.List<ConversaSummaryResponse>> listarConversas(Authentication auth) {
        return ResponseEntity.ok(tutorService.listarConversas(auth.getName()));
    }

    @PostMapping("/conversas")
    @Operation(summary = "Cria uma nova conversa com o Tutor IA")
    public ResponseEntity<ConversaSummaryResponse> criarConversa(
            Authentication auth, @Valid @RequestBody(required = false) CriarConversaRequest request) {
        String titulo = request != null ? request.titulo() : null;
        return ResponseEntity.ok(tutorService.criarConversa(auth.getName(), titulo));
    }

    @GetMapping("/conversas/{id}")
    @Operation(summary = "Devolve uma conversa com o respetivo histórico de mensagens")
    public ResponseEntity<ConversaDetailResponse> obterConversa(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(tutorService.obterConversa(auth.getName(), id));
    }

    @DeleteMapping("/conversas/{id}")
    @Operation(summary = "Elimina uma conversa e todo o seu histórico")
    public ResponseEntity<Void> eliminarConversa(Authentication auth, @PathVariable UUID id) {
        tutorService.eliminarConversa(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Chat
    // ------------------------------------------------------------------

    @PostMapping("/chat")
    @Operation(summary = "Envia uma mensagem ao Tutor IA e recebe a resposta completa (sem streaming)")
    public ResponseEntity<MensagemResponse> chat(Authentication auth, @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(tutorService.perguntar(auth.getName(), request));
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    @Operation(summary = "Envia uma mensagem ao Tutor IA e transmite a resposta em tempo real (Server-Sent Events)")
    public SseEmitter chatStream(
            Authentication auth,
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse response) {
        // Anti-buffer: proxies (Nginx/Cloudflare) não devem acumular o stream
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        return tutorService.perguntarStream(auth.getName(), request);
    }

    // ------------------------------------------------------------------
    // Geração de conteúdo de estudo
    // ------------------------------------------------------------------

    @PostMapping("/resumo")
    @Operation(summary = "Gera um resumo de um diploma, artigo ou texto livre")
    public ResponseEntity<ResumoIAResponse> resumir(@Valid @RequestBody ResumoIARequest request) {
        return ResponseEntity.ok(tutorService.resumir(request));
    }

    @PostMapping("/explicar")
    @Operation(summary = "Explica um artigo (ou um trecho selecionado dele) de forma didática")
    public ResponseEntity<ExplicarArtigoResponse> explicar(@Valid @RequestBody ExplicarArtigoRequest request) {
        return ResponseEntity.ok(tutorService.explicarArtigo(request));
    }

    @PostMapping("/flashcards")
    @Operation(summary = "Gera flashcards de estudo a partir de um diploma ou artigo, opcionalmente guardando-os")
    public ResponseEntity<GerarFlashcardsResponse> gerarFlashcards(@Valid @RequestBody GerarFlashcardsRequest request) {
        return ResponseEntity.ok(tutorService.gerarFlashcards(request));
    }

    @PostMapping("/questoes")
    @Operation(summary = "Gera questões de escolha múltipla a partir de um diploma ou artigo, opcionalmente guardando-as")
    public ResponseEntity<GerarQuestoesResponse> gerarQuestoes(@Valid @RequestBody GerarQuestoesRequest request) {
        return ResponseEntity.ok(tutorService.gerarQuestoes(request));
    }

    // ------------------------------------------------------------------
    // Diagnóstico
    // ------------------------------------------------------------------

    @GetMapping("/status")
    @Operation(summary = "Verifica se o provider de IA configurado está acessível e devolve limites")
    public ResponseEntity<Map<String, Object>> status(HttpServletRequest request) {
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("provider", tutorService.nomeProvider());
        body.put("disponivel", tutorService.iaDisponivel());
        body.putAll(iaRateLimitFilter.snapshot(request));
        body.putAll(iaQuotaState.asMap());
        return ResponseEntity.ok(body);
    }
}
