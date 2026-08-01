package ao.magistratura.controller;

import ao.magistratura.dto.documento.AtualizarDocumentoRequest;
import ao.magistratura.dto.documento.DocumentoResponse;
import ao.magistratura.dto.documento.IndiceItemResponse;
import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Documento;
import ao.magistratura.service.DocumentoService;
import ao.magistratura.service.DocumentoProgressHub;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import ao.magistratura.pipeline.audit.PipelineAuditService;
import ao.magistratura.pipeline.audit.PipelineAuditoria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Biblioteca — Documentos", description = "Importação, processamento e consulta de PDFs jurídicos")
@SecurityRequirement(name = "bearerAuth")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final DocumentoProgressHub documentoProgressHub;
    private final PipelineAuditService pipelineAuditService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa um novo PDF para a Biblioteca Jurídica (não extrai artigos ainda)")
    public ResponseEntity<DocumentoResponse> importar(
            @RequestParam MultipartFile ficheiro,
            @RequestParam String titulo,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String fonte,
            @RequestParam(defaultValue = "true") boolean oficial,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataPublicacao
    ) {
        Documento documento = documentoService.importar(ficheiro, categoriaId, titulo, fonte, oficial, dataPublicacao);
        return ResponseEntity.status(201).body(mapResponse(documento));
    }

    @PostMapping("/{id}/processar")
    @Operation(summary = "Extrai o texto do PDF e estrutura-o em artigos, associados ao diploma indicado")
    public ResponseEntity<DocumentoResponse> processar(
            @PathVariable UUID id,
            @RequestParam UUID diplomaId
    ) {
        Documento documento = documentoService.processar(id, diplomaId);
        return ResponseEntity.ok(mapResponse(documento));
    }

    @GetMapping(value = "/{id}/progress", produces = "text/event-stream")
    @Operation(summary = "SSE — progresso do processamento do documento em tempo real")
    public SseEmitter progresso(@PathVariable UUID id, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        // Garante que o documento existe (404 se não)
        documentoService.obter(id);
        return documentoProgressHub.subscribe(id);
    }

    @GetMapping
    @Operation(summary = "Lista documentos. Filtros: termo, categoriaId, diplomaId. Suporta paginação.")
    public ResponseEntity<Page<DocumentoResponse>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID diplomaId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(documentoService.listar(termo, categoriaId, diplomaId, pageable).map(this::mapResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um documento")
    public ResponseEntity<DocumentoResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(mapResponse(documentoService.obter(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza metadados de um documento (título, categoria, fonte, oficial, data de publicação)")
    public ResponseEntity<DocumentoResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody AtualizarDocumentoRequest request
    ) {
        return ResponseEntity.ok(mapResponse(documentoService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina definitivamente um documento: remove artigos e embeddings derivados, e o ficheiro PDF do disco. forcar=true ignora bloqueio de processamento.")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean forcar) {
        documentoService.eliminar(id, forcar);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Devolve o ficheiro PDF original para visualização/download")
    public ResponseEntity<FileSystemResource> abrirPdf(@PathVariable UUID id) {
        File ficheiro = documentoService.obterFicheiro(id);
        FileSystemResource recurso = new FileSystemResource(ficheiro);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + ficheiro.getName() + "\"")
                .body(recurso);
    }

@GetMapping("/{id}/indice")
@Operation(summary = "Índice navegável do documento: artigos agrupados por capítulo/secção, na ordem do PDF")
public ResponseEntity<List<IndiceItemResponse>> obterIndice(@PathVariable UUID id) {

    List<Artigo> artigos = documentoService.obterIndice(id);

    List<IndiceItemResponse> indice = artigos.stream()
            .map(this::mapIndiceItem)
            .toList();

    return ResponseEntity.ok(indice);
    }

    private DocumentoResponse mapResponse(Documento d) {
        String estado = d.getEstado() != null ? d.getEstado().name() : null;
        String codigo = derivarCodigoErro(d);
        return new DocumentoResponse(
                d.getId(),
                d.getTitulo(),
                d.getCategoria() != null ? d.getCategoria().getId() : null,
                d.getCategoria() != null ? d.getCategoria().getNome() : null,
                d.getDiploma() != null ? d.getDiploma().getId() : null,
                d.getDiploma() != null ? d.getDiploma().getTitulo() : null,
                d.getVersao(),
                estado,
                rotuloEstado(estado),
                d.getFonte(),
                d.getOficial(),
                d.getDataPublicacao(),
                d.getDataImportacao(),
                d.getNumeroPaginas(),
                d.getRevisaoNecessaria(),
                mensagemAmigavel(d),
                d.getProgressoPaginasOk(),
                d.getProgressoPaginasTotal(),
                d.getProgressoPercentagem(),
                resumoResultado(d),
                d.getTipoPdf(),
                codigo,
                d.getObservacoesProcessamento(),
                acoesSugeridas(codigo, d)
        );
    }

    private static String rotuloEstado(String estado) {
        if (estado == null) return "Desconhecido";
        return switch (estado) {
            case "IMPORTADO" -> "Importado";
            case "PROCESSANDO", "ANALISANDO", "EXTRAINDO_TEXTO", "ESTRUTURANDO" -> "A processar";
            case "OCR_EM_EXECUCAO" -> "A ler páginas (OCR)";
            case "PROCESSADO" -> "Pronto";
            case "PROCESSADO_COM_AVISOS" -> "Pronto (com avisos)";
            case "FALHA_EXTRACAO" -> "Não foi possível extrair texto";
            case "ERRO" -> "Erro";
            default -> estado;
        };
    }

    private static String mensagemAmigavel(Documento d) {
        if (d.getMensagemProgresso() != null && !d.getMensagemProgresso().isBlank()) {
            return d.getMensagemProgresso();
        }
        // Em falha, expor observações (orientação ao utilizador) em vez de ocultá-las
        if (d.getEstado() == ao.magistratura.entity.EstadoDocumento.FALHA_EXTRACAO
                || d.getEstado() == ao.magistratura.entity.EstadoDocumento.ERRO) {
            if (d.getObservacoesProcessamento() != null && !d.getObservacoesProcessamento().isBlank()) {
                return d.getObservacoesProcessamento();
            }
        }
        return null;
    }

    private static String resumoResultado(Documento d) {
        String e = d.getEstado() != null ? d.getEstado().name() : "";
        if ("PROCESSADO".equals(e) || "PROCESSADO_COM_AVISOS".equals(e)) {
            if (Boolean.TRUE.equals(d.getRevisaoNecessaria())) {
                return "Processado — recomenda-se revisão dos artigos";
            }
            return "Processado com sucesso";
        }
        if ("FALHA_EXTRACAO".equals(e) || "ERRO".equals(e)) {
            if (d.getMensagemProgresso() != null) return d.getMensagemProgresso();
            if (d.getObservacoesProcessamento() != null) return d.getObservacoesProcessamento();
            return "Não foi possível concluir o processamento";
        }
        return null;
    }

    private static String derivarCodigoErro(Documento d) {
        if (d.getEstado() != ao.magistratura.entity.EstadoDocumento.FALHA_EXTRACAO
                && d.getEstado() != ao.magistratura.entity.EstadoDocumento.ERRO) {
            return null;
        }
        if ("PROTECTED".equalsIgnoreCase(d.getTipoPdf())) {
            return "PDF_PROTECTED_BLOCKED";
        }
        String obs = ((d.getObservacoesProcessamento() != null ? d.getObservacoesProcessamento() : "")
                + " " + (d.getMensagemProgresso() != null ? d.getMensagemProgresso() : "")).toLowerCase();
        if (obs.contains("protegido") || obs.contains("restriç") || obs.contains("imprimir para pdf")
                || obs.contains("password") || obs.contains("seleccionável") || obs.contains("selecionável")) {
            return "PDF_PROTECTED_BLOCKED";
        }
        if (obs.contains("ocr") || obs.contains("scan") || obs.contains("imagem")) {
            return "PDF_OCR_FAILED";
        }
        return "PROCESSAMENTO_FALHOU";
    }

    private static List<String> acoesSugeridas(String codigo, Documento d) {
        if (codigo == null) return List.of();
        if (codigo.startsWith("PDF_PROTECTED")) {
            return List.of(
                    "Abra o PDF no Chrome, Edge ou Adobe Reader",
                    "Escolha Imprimir → «Guardar como PDF» / «Microsoft Print to PDF»",
                    "Elimine este documento e importe a nova versão sem protecção",
                    "Volte a processar associado ao mesmo diploma"
            );
        }
        if ("PDF_OCR_FAILED".equals(codigo)) {
            return List.of(
                    "Confirme que o PDF não está danificado",
                    "Tente uma digitalização com melhor qualidade",
                    "Se tiver uma versão com texto seleccionável, use-a"
            );
        }
        return List.of("Tente reprocessar", "Se o problema continuar, reimporte o PDF");
    }

    private IndiceItemResponse mapIndiceItem(Artigo a) {
        return new IndiceItemResponse(
                a.getId(),
                a.getCapitulo(),
                a.getSeccao(),
                a.getNumero(),
                a.getTitulo(),
                a.getOrdem(),
                a.getPaginaInicio()
        );
    }


    @PostMapping("/{id}/reprocessar")
    @Operation(summary = "Reprocessa o documento mantendo o PDF: limpa derivados e executa o pipeline de novo")
    public ResponseEntity<DocumentoResponse> reprocessar(@PathVariable UUID id) {
        return ResponseEntity.ok(mapResponse(documentoService.reprocessar(id)));
    }

    @GetMapping("/{id}/pipeline/auditoria")
    @Operation(summary = "Histórico de etapas do pipeline de processamento do documento")
    public ResponseEntity<List<PipelineAuditoria>> historicoPipeline(@PathVariable UUID id) {
        documentoService.obter(id);
        return ResponseEntity.ok(pipelineAuditService.historico(id));
    }

}