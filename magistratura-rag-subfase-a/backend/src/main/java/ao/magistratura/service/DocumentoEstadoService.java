package ao.magistratura.service;

import ao.magistratura.entity.Documento;
import ao.magistratura.entity.EstadoDocumento;
import ao.magistratura.pipeline.DocumentPipelineOrchestrator;
import ao.magistratura.pipeline.model.PipelineContexto;
import ao.magistratura.repository.DiplomaRepository;
import ao.magistratura.repository.DocumentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import java.util.Set;
import java.util.UUID;

/**
 * Isola a execução do pipeline e a persistência de falhas.
 * <p>
 * O pipeline <strong>não</strong> corre dentro de uma {@code @Transactional} longa:
 * isso segurava o lock da linha {@code documentos} e deadlocked com
 * {@link DocumentoProgressoService} ({@code REQUIRES_NEW}).
 * {@link #marcarErro} usa {@code REQUIRES_NEW} para gravar ERRO mesmo se o
 * caller falhar a seguir.
 */
@Service
public class DocumentoEstadoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoEstadoService.class);

    /** Minutos máximos para um pipeline (OCR longo). 0 = sem limite. */
    @Value("${app.pipeline.timeout-minutes:45}")
    private int timeoutMinutes;

    /** Estados em que o documento ainda não terminou — elegíveis para marcar ERRO. */
    private static final Set<EstadoDocumento> ESTADOS_INTERMEDIOS = EnumSet.of(
            EstadoDocumento.PROCESSANDO,
            EstadoDocumento.ANALISANDO,
            EstadoDocumento.EXTRAINDO_TEXTO,
            EstadoDocumento.OCR_EM_EXECUCAO,
            EstadoDocumento.ESTRUTURANDO
    );

    private final DocumentoRepository documentoRepository;
    private final DiplomaRepository diplomaRepository;
    private final DocumentPipelineOrchestrator documentPipelineOrchestrator;

    public DocumentoEstadoService(DocumentoRepository documentoRepository,
                                  DiplomaRepository diplomaRepository,
                                  DocumentPipelineOrchestrator documentPipelineOrchestrator) {
        this.documentoRepository = documentoRepository;
        this.diplomaRepository = diplomaRepository;
        this.documentPipelineOrchestrator = documentPipelineOrchestrator;
    }

    /**
     * Corre o pipeline <strong>sem</strong> TX envolvente.
     * Cada save (repositório / progresso) usa TX curta própria.
     * Em falha o caller invoca {@link #marcarErro} numa TX nova.
     */
    public void executarPipeline(UUID documentoId, UUID diplomaId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalStateException("Documento não encontrado"));
        var diploma = diplomaRepository.findById(diplomaId)
                .orElseThrow(() -> new IllegalStateException("Diploma não encontrado"));
        File ficheiro = new File(documento.getCaminhoFicheiro());
        if (!ficheiro.isFile()) {
            throw new IllegalStateException(
                    "PDF não encontrado em disco: " + documento.getCaminhoFicheiro()
                    + ". Reimporte o documento.");
        }
        PipelineContexto ctx = new PipelineContexto();
        ctx.setDocumento(documento);
        ctx.setDiploma(diploma);
        ctx.setFicheiro(ficheiro);
        log.info("Pipeline a iniciar documento={} ficheiro={} bytes={}",
                documentoId, ficheiro.getName(), ficheiro.length());
        correrComTimeout(() -> documentPipelineOrchestrator.executar(ctx), documentoId);
    }

    /**
     * Corre o reprocessamento (limpa derivados + pipeline) sem TX envolvente.
     */
    public void reprocessarPipeline(UUID documentoId, UUID diplomaId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalStateException("Documento não encontrado"));
        var diploma = diplomaRepository.findById(diplomaId)
                .orElseThrow(() -> new IllegalStateException("Diploma não encontrado"));
        File ficheiro = new File(documento.getCaminhoFicheiro());
        if (!ficheiro.isFile()) {
            throw new IllegalStateException(
                    "PDF não encontrado em disco: " + documento.getCaminhoFicheiro());
        }
        PipelineContexto ctx = new PipelineContexto();
        ctx.setDocumento(documento);
        ctx.setDiploma(diploma);
        ctx.setFicheiro(ficheiro);
        log.info("Reprocessamento a iniciar documento={}", documentoId);
        correrComTimeout(() -> documentPipelineOrchestrator.reprocessar(ctx), documentoId);
    }

    private void correrComTimeout(Runnable pipeline, UUID documentoId) {
        if (timeoutMinutes <= 0) {
            pipeline.run();
            return;
        }
        ExecutorService es = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pipeline-run-" + documentoId);
            t.setDaemon(true);
            return t;
        });
        Future<?> fut = es.submit(pipeline);
        try {
            fut.get(timeoutMinutes, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            fut.cancel(true);
            throw new IllegalStateException(
                    "Processamento excedeu " + timeoutMinutes
                    + " minutos (OCR/PDF muito pesado ou Tesseract bloqueado). "
                    + "Instale tesseract-ocr + por.traineddata, ou use um PDF com texto seleccionável.");
        } catch (ExecutionException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(c.getMessage() != null ? c.getMessage() : "Falha no pipeline", c);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        } finally {
            es.shutdownNow();
        }
    }

    /**
     * Persiste ERRO numa transacção <strong>nova</strong>, independente do
     * rollback da TX do pipeline. Só actualiza se o documento ainda estiver
     * num estado intermédio (não sobrescreve PROCESSADO / FALHA_EXTRACAO já
     * commitados noutro fluxo).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarErro(UUID documentoId, String mensagem) {
        documentoRepository.findById(documentoId).ifPresent(doc -> {
            if (!ESTADOS_INTERMEDIOS.contains(doc.getEstado())) {
                log.info("marcarErro ignorado documento={} estado actual={}", documentoId, doc.getEstado());
                return;
            }
            doc.setEstado(EstadoDocumento.ERRO);
            String msg = (mensagem != null && !mensagem.isBlank())
                    ? mensagem
                    : "Não foi possível concluir o processamento.";
            doc.setMensagemProgresso(truncar(msg, 200));
            doc.setObservacoesProcessamento(msg);
            documentoRepository.save(doc);
            log.warn("Documento {} marcado ERRO (REQUIRES_NEW): {}", documentoId, doc.getMensagemProgresso());
        });
    }

    /**
     * Persiste falha de extracção (PDF protegido, scan ilegível, etc.) numa TX nova.
     * Distinto de {@link #marcarErro}: a UI mostra orientação de recuperação, não um erro genérico.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFalhaExtracao(UUID documentoId, String mensagem) {
        documentoRepository.findById(documentoId).ifPresent(doc -> {
            // Permite actualizar a partir de estados intermédios OU se já ficou inconsistente
            if (doc.getEstado() == EstadoDocumento.PROCESSADO
                    || doc.getEstado() == EstadoDocumento.PROCESSADO_COM_AVISOS) {
                log.info("marcarFalhaExtracao ignorado documento={} estado={}", documentoId, doc.getEstado());
                return;
            }
            String msg = (mensagem != null && !mensagem.isBlank())
                    ? mensagem
                    : "Não foi possível extrair o texto deste PDF.";
            doc.setEstado(EstadoDocumento.FALHA_EXTRACAO);
            doc.setRevisaoNecessaria(true);
            doc.setMensagemProgresso(truncar(msg, 200));
            doc.setObservacoesProcessamento(msg);
            doc.setProgressoPercentagem(0);
            documentoRepository.save(doc);
            log.warn("Documento {} marcado FALHA_EXTRACAO: {}", documentoId, doc.getMensagemProgresso());
        });
    }

    private static String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Heurística: mensagens de extracção/protecção não devem ser gravadas como ERRO genérico. */
    public static boolean isFalhaExtracao(String mensagem) {
        if (mensagem == null) return false;
        String m = mensagem.toLowerCase();
        return m.contains("protegido")
                || m.contains("restriç")
                || m.contains("restric")
                || m.contains("password")
                || m.contains("seleccionável")
                || m.contains("selecionável")
                || m.contains("imprimir para pdf")
                || m.contains("texto nativo insuficiente")
                || m.contains("impossível estruturar")
                || m.contains("falha extrac");
    }
}
