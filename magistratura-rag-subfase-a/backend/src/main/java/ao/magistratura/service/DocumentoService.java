package ao.magistratura.service;

import ao.magistratura.dto.documento.AtualizarDocumentoRequest;
import ao.magistratura.entity.*;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.TopicoArtigoRepository;
import ao.magistratura.repository.CategoriaRepository;
import ao.magistratura.repository.DiplomaRepository;
import ao.magistratura.repository.DocumentoEmbeddingRepository;
import ao.magistratura.repository.DocumentoRepository;
import ao.magistratura.knowledge.vector.VectorStore;
import ao.magistratura.pipeline.DocumentPipelineOrchestrator;
// job assíncrono — ver DocumentoProcessamentoJob
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.service.pdf.ArtigoExtraido;
import ao.magistratura.service.pdf.EstruturaJuridicaParser;
import ao.magistratura.service.pdf.PaginaTexto;
import ao.magistratura.service.pdf.PdfExtractorService;
import ao.magistratura.service.pdf.PdfLoadHelper;
import ao.magistratura.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Orquestra o ciclo de vida de um {@link Documento} (PDF) da Biblioteca
 * Jurídica: importação, extração de texto, estruturação em artigos e
 * validação da qualidade da extração.
 * <p>
 * Segue o mesmo estilo do {@link BibliotecaService} já existente
 * (métodos de leitura simples, DTOs mapeados manualmente), mas com um
 * fluxo de escrita adicional para importação/processamento.
 */
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    /**
     * Se a contagem estruturada de artigos ficar abaixo desta fração da
     * contagem solta (heurística, ver {@link EstruturaJuridicaParser}),
     * assume-se que o parser não conseguiu estruturar uma parte relevante
     * do documento e marca-se para revisão manual, em vez de publicar
     * silenciosamente um diploma incompleto.
     */
    private static final double LIMIAR_REVISAO = 0.6;

    private static final long TAMANHO_MAXIMO_BYTES = 50L * 1024 * 1024; // 50 MB

    @Value("${app.biblioteca.storage-path:./storage/documentos}")
    private String storagePath;

    private final DocumentoRepository documentoRepository;
    private final DiplomaRepository diplomaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ArtigoRepository artigoRepository;
    private final TopicoArtigoRepository topicoArtigoRepository;
    private final DocumentoEmbeddingRepository documentoEmbeddingRepository;

    private final PdfExtractorService pdfExtractorService;
    private final EstruturaJuridicaParser estruturaJuridicaParser;
    private final DocumentPipelineOrchestrator documentPipelineOrchestrator;
    private final DocumentoProcessamentoJob documentoProcessamentoJob;
    private final ObjectProvider<VectorStore> vectorStore;
    private final DocumentoProgressHub documentoProgressHub;

    // ---------- Importação ----------

    @Transactional
    public Documento importar(MultipartFile ficheiro, UUID categoriaId, String titulo,
                               String fonte, boolean oficial, LocalDate dataPublicacao) {
        if (ficheiro.isEmpty()) {
            throw new RegraNegocioException("O ficheiro enviado está vazio.");
        }
        if (ficheiro.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraNegocioException("O ficheiro excede o tamanho máximo permitido (50 MB).");
        }
        String contentType = ficheiro.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new RegraNegocioException("Apenas ficheiros PDF são aceites.");
        }

        try {
            Path pasta = Path.of(storagePath);
            Files.createDirectories(pasta);

            // Guarda primeiro num ficheiro temporário para poder calcular o hash
            // e o número de páginas antes de decidir o nome definitivo.
            File temporario = File.createTempFile("upload-", ".pdf");
            ficheiro.transferTo(temporario);

            String hash = HashUtil.sha256(temporario);
            documentoRepository.findByHashFicheiro(hash).ifPresent(existente -> {
                temporario.delete();
                throw new RegraNegocioException(
                        "Este ficheiro já foi importado anteriormente (documento " + existente.getId() + ").");
            });

            int numeroPaginas = pdfExtractorService.contarPaginas(temporario);

            String nomeFinal = hash + ".pdf";
            Path destino = pasta.resolve(nomeFinal);
            Files.move(temporario.toPath(), destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Categoria categoria = null;
            if (categoriaId != null) {
                categoria = categoriaRepository.findById(categoriaId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
            }

            Documento documento = Documento.builder()
                    .titulo(titulo)
                    .categoria(categoria)
                    .estado(EstadoDocumento.IMPORTADO)
                    .fonte(fonte)
                    .oficial(oficial)
                    .dataPublicacao(dataPublicacao)
                    .hashFicheiro(hash)
                    .numeroPaginas(numeroPaginas)
                    .caminhoFicheiro(destino.toString())
                    .tamanhoBytes(ficheiro.getSize())
                    .build();

            // Pré-inspecção leve (não bloqueia o import)
            try (var docPdf = PdfLoadHelper.loadComTimeout(destino.toFile(), 15)) {
                var perm = PdfLoadHelper.inspeccionar(docPdf);
                if (perm.bloqueadoTotalmente()) {
                    documento.setTipoPdf("PROTECTED");
                    documento.setObservacoesProcessamento(perm.mensagemUtilizador());
                    documento.setMensagemProgresso(
                            "PDF bloqueado — exporte sem restrições («Imprimir para PDF») antes de processar.");
                } else if (perm.encriptado() && !perm.podeExtrairTexto() && perm.podeImprimir()) {
                    // Caso típico Diário da República: será OCR, não falha imediata
                    documento.setTipoPdf("IMAGE");
                    documento.setMensagemProgresso(
                            "PDF oficial com restrição de cópia — será lido por OCR ao processar.");
                } else if (perm.encriptado() && !perm.podeExtrairTexto()) {
                    documento.setTipoPdf("PROTECTED");
                    documento.setObservacoesProcessamento(perm.mensagemUtilizador());
                    documento.setMensagemProgresso(
                            "PDF com restrições — confirme se consegue «Imprimir para PDF».");
                }
            } catch (Exception ex) {
                log.warn("Pré-inspecção PDF não concluída no import: {}", ex.getMessage());
            }

            return documentoRepository.save(documento);

        } catch (IOException e) {
            log.error("Falha ao importar documento '{}': {}", titulo, e.getMessage());
            throw new RegraNegocioException("Não foi possível processar o ficheiro enviado: " + e.getMessage());
        }
    }

    // ---------- Processamento ----------

    /**
     * Extrai o texto do PDF, estrutura-o em artigos e liga-os ao diploma
     * indicado. Não bloqueia em caso de desvio na contagem de artigos —
     * marca {@code revisaoNecessaria} e continua, para não travar o fluxo
     * de trabalho de um único importador (ver discussão da Etapa 7).
     */
    /**
     * Processa o documento via {@link DocumentPipelineOrchestrator}
     * (pipeline modular Fase 3). A API pública permanece inalterada.
     */
    /**
     * Agenda o pipeline em background e devolve de imediato o documento em estado PROCESSANDO.
     * Evita timeouts HTTP (OCR de dezenas de páginas pode demorar minutos).
     */
    @Transactional
    public Documento processar(UUID documentoId, UUID diplomaId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));

        if (documento.getEstado() == EstadoDocumento.PROCESSANDO) {
            throw new RegraNegocioException("Este documento já está a ser processado.");
        }

        Diploma diploma = diplomaRepository.findById(diplomaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Diploma não encontrado. Cria um diploma (botão «Novo diploma») antes de processar."));

        File ficheiro = new File(documento.getCaminhoFicheiro());
        if (!ficheiro.isFile()) {
            throw new RegraNegocioException(
                    "O ficheiro PDF não foi encontrado no disco (" + documento.getCaminhoFicheiro()
                            + "). Reimporta o documento.");
        }

        documento.setDiploma(diploma);
        documento.setEstado(EstadoDocumento.PROCESSANDO);
        try {
            documento.setMensagemProgresso("A iniciar processamento…");
            documento.setProgressoPercentagem(0);
            documento.setProgressoPaginasOk(0);
            documento.setProgressoPaginasTotal(null);
        } catch (Exception ignored) {
            // campos de progresso podem não existir em schemas antigos
        }
        documento.setObservacoesProcessamento(null);
        documento = documentoRepository.save(documento);

        // CRÍTICO: só disparar o @Async DEPOIS do commit desta transacção.
        // Se o job arrancar antes do commit, a outra thread pode:
        //  (1) não ver o estado PROCESSANDO / diploma associado (isolamento),
        //  (2) bloquear à espera do lock da linha documentos (hang silencioso),
        // ficando a mensagem eternamente em "A iniciar processamento…" a 0%.
        final UUID docIdFinal = documentoId;
        final UUID diplomaIdFinal = diplomaId;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("TX commitada — a disparar job pipeline documento={} diploma={}",
                            docIdFinal, diplomaIdFinal);
                    documentoProcessamentoJob.executar(docIdFinal, diplomaIdFinal);
                }
            });
            log.info("Processamento agendado (após commit) documento={} diploma={}", documentoId, diplomaId);
        } else {
            // Sem TX activa (testes / chamada interna) — dispara de imediato
            log.info("Sem TX activa — a disparar job pipeline de imediato documento={}", documentoId);
            documentoProcessamentoJob.executar(documentoId, diplomaId);
        }
        return documento;
    }


    /**
     * Reprocessa sem novo upload: limpa artigos/índices derivados e corre o pipeline de novo.
     */
    @Transactional
    public Documento reprocessar(UUID documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
        if (documento.getDiploma() == null) {
            throw new RegraNegocioException("Associe um diploma antes de reprocessar (ou use processar com diplomaId).");
        }
        if (documento.getEstado() == EstadoDocumento.PROCESSANDO) {
            throw new RegraNegocioException("Este documento já está a ser processado.");
        }
        File ficheiro = new File(documento.getCaminhoFicheiro());
        if (!ficheiro.isFile()) {
            throw new RegraNegocioException("O ficheiro PDF não foi encontrado no disco. Reimporta o documento.");
        }
        UUID diplomaId = documento.getDiploma().getId();
        documento.setEstado(EstadoDocumento.PROCESSANDO);
        try {
            documento.setMensagemProgresso("A reiniciar processamento…");
            documento.setProgressoPercentagem(0);
            documento.setProgressoPaginasOk(0);
            documento.setProgressoPaginasTotal(null);
        } catch (Exception ignored) {
        }
        documento = documentoRepository.save(documento);
        documentoProgressHub.publishProgress(documentoId, "PROCESSANDO", "A reiniciar processamento…", 0, null);
        final UUID docR = documentoId;
        final UUID dipR = diplomaId;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("TX commitada — a disparar job reprocessamento documento={}", docR);
                    documentoProcessamentoJob.reprocessar(docR, dipR);
                }
            });
        } else {
            documentoProcessamentoJob.reprocessar(documentoId, diplomaId);
        }
        return documento;
    }

    // ---------- Atualização ----------

    /**
     * Atualiza metadados de um documento (título, categoria, fonte, oficial,
     * data de publicação). Campos nulos no pedido são ignorados — não muda
     * ficheiro, diploma nem estado, que continuam a ser geridos por
     * /processar e /reprocessar.
     */
    @Transactional
    public Documento atualizar(UUID id, AtualizarDocumentoRequest request) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));

        if (request.titulo() != null && !request.titulo().isBlank()) {
            documento.setTitulo(request.titulo());
        }
        if (request.categoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
            documento.setCategoria(categoria);
        }
        if (request.fonte() != null) {
            documento.setFonte(request.fonte());
        }
        if (request.oficial() != null) {
            documento.setOficial(request.oficial());
        }
        if (request.dataPublicacao() != null) {
            documento.setDataPublicacao(request.dataPublicacao());
        }

        return documentoRepository.save(documento);
    }

    // ---------- Eliminação ----------

    /**
     * Elimina definitivamente um documento: embeddings e artigos derivados
     * do PDF, o registo em {@code documentos} e o ficheiro físico em disco.
     * pipeline_auditoria/pipeline_metricas saem sozinhos (ON DELETE CASCADE);
     * knowledge_origin fica com documento_id a NULL (ON DELETE SET NULL).
     */
    @Transactional
    public void eliminar(UUID id) {
        eliminar(id, false);
    }

    @Transactional
    public void eliminar(UUID id, boolean forcar) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));

        // Bloqueia só se o job parece activo (percentagem 1–99). Documentos
        // presos a 0% ou sem percentagem podem ser eliminados (job morto / rollback).
        // forcar=true permite eliminar mesmo em processamento (documento preso).
        if (!forcar && isProcessamentoActivo(documento)) {
            throw new RegraNegocioException(
                    "Este documento está a ser processado; aguarde antes de eliminar. "
                    + "Se estiver preso, use Eliminar (forçar) ou ?forcar=true.");
        }
        if (isEstadoIntermedio(documento.getEstado())) {
            log.warn("Eliminação forçada de documento {} em estado intermédio {} (job provavelmente parado)",
                    id, documento.getEstado());
        }

        Diploma diploma = documento.getDiploma();
        UUID diplomaId = diploma != null ? diploma.getId() : null;
        String caminho = documento.getCaminhoFicheiro();

        // Índice vetorial (pgvector / noop)
        VectorStore vs = vectorStore.getIfAvailable();
        if (vs != null) {
            try {
                vs.deleteByDocumentoId(id);
            } catch (Exception e) {
                log.warn("Falha ao limpar vectores do documento {}: {}", id, e.getMessage());
            }
        }

        documentoEmbeddingRepository.deleteByArtigoDocumentoId(id);
        try {
            topicoArtigoRepository.deleteByDocumentoId(id);
        } catch (Exception e) {
            log.warn("Limpeza topico_artigo documento {}: {}", id, e.getMessage());
        }
        artigoRepository.deleteByDocumentoId(id);
        documentoRepository.delete(documento);

        // Biblioteca lista Diplomas — se não restam documentos nem artigos, remove o diploma órfão
        if (diplomaId != null) {
            long docsRestantes = documentoRepository.countByDiplomaId(diplomaId);
            if (docsRestantes == 0) {
                artigoRepository.deleteByDiplomaId(diplomaId);
                diplomaRepository.deleteById(diplomaId);
                log.info("Diploma {} eliminado (órfão após remoção do documento {})", diplomaId, id);
            }
        }

        if (caminho != null) {
            File ficheiro = new File(caminho);
            if (ficheiro.exists() && !ficheiro.delete()) {
                log.warn("Documento {} removido da base de dados, mas não foi possível apagar o ficheiro físico '{}'",
                        id, caminho);
            }
        }
    }

    /**
     * Heurística sem coluna de timestamp: considera o job activo apenas se o
     * estado é intermédio e a percentagem está entre 1 e 99 (trabalho em curso).
     * 0% ou null → documento provavelmente preso após falha/rollback.
     */
    private static boolean isProcessamentoActivo(Documento documento) {
        if (!isEstadoIntermedio(documento.getEstado())) {
            return false;
        }
        Integer pct = null;
        try {
            pct = documento.getProgressoPercentagem();
        } catch (Exception ignored) {
            // schema antigo sem o campo
        }
        return pct != null && pct > 0 && pct < 100;
    }

    private static boolean isEstadoIntermedio(EstadoDocumento estado) {
        if (estado == null) {
            return false;
        }
        return switch (estado) {
            case PROCESSANDO, ANALISANDO, EXTRAINDO_TEXTO, OCR_EM_EXECUCAO, ESTRUTURANDO -> true;
            default -> false;
        };
    }

    // ---------- Leitura ----------

    @Transactional(readOnly = true)
    public Page<Documento> listar(String termo, UUID categoriaId, UUID diplomaId, Pageable pageable) {
        if (termo != null && !termo.isBlank()) {
            return documentoRepository.pesquisar(termo.trim(), pageable);
        }
        if (diplomaId != null) {
            return documentoRepository.findByDiplomaId(diplomaId, pageable);
        }
        if (categoriaId != null) {
            return documentoRepository.findByCategoriaId(categoriaId, pageable);
        }
        return documentoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Documento obter(UUID id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
    }

    @Transactional(readOnly = true)
    public File obterFicheiro(UUID id) {
        Documento documento = obter(id);
        File ficheiro = new File(documento.getCaminhoFicheiro());
        if (!ficheiro.exists()) {
            throw new RecursoNaoEncontradoException("Ficheiro do documento não encontrado em disco");
        }
        return ficheiro;
    }

    /**
     * Índice navegável do diploma associado ao documento: artigos agrupados
     * pela hierarquia capítulo → secção, na ordem em que aparecem no PDF.
     */
    @Transactional(readOnly = true)
    public List<Artigo> obterIndice(UUID documentoId) {
        obter(documentoId); // valida existência
        return artigoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId);
    }
}