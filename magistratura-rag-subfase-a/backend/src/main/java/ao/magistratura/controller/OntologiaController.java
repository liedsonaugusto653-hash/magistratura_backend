package ao.magistratura.controller;

import ao.magistratura.dto.ontologia.*;
import ao.magistratura.service.OntologiaAutoLigacaoService;
import ao.magistratura.service.OntologiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API da camada conceptual (ontologia jurídica).
 * Estudo por entidade/conceito, não apenas por diploma.
 */
@RestController
@RequestMapping("/api/ontologia")
@RequiredArgsConstructor
@Tag(name = "Ontologia Jurídica", description = "Mapa conceptual: entidades, tópicos e ligações a artigos")
@SecurityRequirement(name = "bearerAuth")
public class OntologiaController {

    private final OntologiaService ontologiaService;
    private final OntologiaAutoLigacaoService autoLigacaoService;

    @GetMapping("/entidades")
    @Operation(summary = "Lista entidades jurídicas conceptuais (Estado, Pessoa, Contrato, …)")
    public ResponseEntity<List<EntidadeJuridicaResponse>> listarEntidades() {
        return ResponseEntity.ok(ontologiaService.listarEntidades());
    }

    @GetMapping("/entidades/{id}")
    @Operation(summary = "Detalhe de uma entidade")
    public ResponseEntity<EntidadeJuridicaResponse> obterEntidade(@PathVariable UUID id) {
        return ResponseEntity.ok(ontologiaService.obterEntidade(id));
    }

    @GetMapping("/entidades/codigo/{codigo}")
    @Operation(summary = "Entidade por código estável (ex.: ESTADO, TRABALHO)")
    public ResponseEntity<EntidadeJuridicaResponse> obterPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(ontologiaService.obterEntidadePorCodigo(codigo.toUpperCase()));
    }

    @GetMapping("/entidades/{id}/mapa")
    @Operation(summary = "Mapa de estudo: entidade + tópicos + artigos ligados")
    public ResponseEntity<MapaConceitoResponse> mapa(@PathVariable UUID id) {
        return ResponseEntity.ok(ontologiaService.mapaPorEntidade(id));
    }

    @GetMapping("/entidades/{id}/topicos")
    @Operation(summary = "Tópicos de uma entidade")
    public ResponseEntity<List<TopicoJuridicoResponse>> topicosPorEntidade(@PathVariable UUID id) {
        return ResponseEntity.ok(ontologiaService.listarTopicosPorEntidade(id));
    }

    @GetMapping("/entidades/{id}/trilha")
    @Operation(summary = "Trilha de estudo sugerida da entidade (ordem topológica a partir de PRESSUPOE)")
    public ResponseEntity<List<TrilhaItemResponse>> trilhaSugerida(@PathVariable UUID id) {
        return ResponseEntity.ok(ontologiaService.obterTrilhaSugerida(id));
    }

    @GetMapping("/topicos")
    @Operation(summary = "Pesquisa tópicos por nome/código/descrição")
    public ResponseEntity<List<TopicoJuridicoResponse>> pesquisar(
            @RequestParam(required = false) String termo) {
        return ResponseEntity.ok(ontologiaService.pesquisarTopicos(termo));
    }

    @GetMapping("/topicos/{id}")
    @Operation(summary = "Detalhe de um tópico (com relações do grafo)")
    public ResponseEntity<TopicoJuridicoResponse> obterTopico(@PathVariable UUID id) {
        return ResponseEntity.ok(ontologiaService.obterTopico(id, true));
    }

    @PostMapping("/topicos/{id}/ficha-estudo")
    @Operation(summary = "Gera (ou devolve em cache) a Ficha de Estudo do tópico: definição + 6 "
            + "perguntas-guia de raciocínio jurídico. Use forcar=true para regenerar.")
    public ResponseEntity<TopicoJuridicoResponse> gerarFichaEstudo(
            @PathVariable UUID id,
            @RequestBody(required = false) GerarFichaEstudoRequest body) {
        boolean forcar = body != null && body.deveForcar();
        return ResponseEntity.ok(ontologiaService.gerarFichaEstudo(id, forcar));
    }

    @GetMapping("/topicos/{id}/artigos")
    @Operation(summary = "Artigos ligados a um tópico conceptual")
    public ResponseEntity<List<TopicoArtigoResponse>> artigosDoTopico(@PathVariable UUID id) {
        return ResponseEntity.ok(ontologiaService.listarArtigosDoTopico(id));
    }

    @PostMapping("/topicos/{id}/artigos")
    @Operation(summary = "Liga um artigo existente a um tópico")
    public ResponseEntity<TopicoArtigoResponse> ligarArtigo(
            @PathVariable UUID id,
            @Valid @RequestBody LigarArtigoRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ontologiaService.ligarArtigo(id, body));
    }

    @DeleteMapping("/topicos/{topicoId}/artigos/{artigoId}")
    @Operation(summary = "Remove a ligação tópico–artigo")
    public ResponseEntity<Void> desligarArtigo(
            @PathVariable UUID topicoId,
            @PathVariable UUID artigoId) {
        ontologiaService.desligarArtigo(topicoId, artigoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auto-ligar")
    @Operation(summary = "Sugere ou cria ligações tópico↔artigo (documento ou diploma). dryRun=true só sugere.")
    public ResponseEntity<List<SugestaoLigacaoResponse>> autoLigar(@RequestBody AutoLigarRequest body) {
        boolean dry = body.dryRun() == null || Boolean.TRUE.equals(body.dryRun());
        if (body.documentoId() != null) {
            return ResponseEntity.ok(autoLigacaoService.sugerirPorDocumento(body.documentoId(), dry));
        }
        if (body.diplomaId() != null) {
            return ResponseEntity.ok(autoLigacaoService.sugerirPorDiploma(body.diplomaId(), dry));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/auto-ligar-biblioteca")
    @Operation(summary = "Liga artigos de toda a biblioteca aos tópicos (PDFs já processados). dryRun=true só sugere.")
    public ResponseEntity<List<SugestaoLigacaoResponse>> autoLigarBiblioteca(
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(autoLigacaoService.ligarTodaBiblioteca(dryRun));
    }
}

