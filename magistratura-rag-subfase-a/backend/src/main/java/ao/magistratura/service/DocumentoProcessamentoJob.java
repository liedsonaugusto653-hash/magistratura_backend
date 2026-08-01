package ao.magistratura.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Job assíncrono do pipeline de documentos.
 * <p>
 * A execução do pipeline corre numa TX (via {@link DocumentoEstadoService});
 * em falha o estado ERRO é gravado noutra TX ({@code REQUIRES_NEW}) para não
 * ser apagado pelo rollback da TX do pipeline.
 */
@Service
public class DocumentoProcessamentoJob {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProcessamentoJob.class);

    private final DocumentoEstadoService documentoEstadoService;
    private final DocumentoProgressHub documentoProgressHub;

    public DocumentoProcessamentoJob(DocumentoEstadoService documentoEstadoService,
                                     DocumentoProgressHub documentoProgressHub) {
        this.documentoEstadoService = documentoEstadoService;
        this.documentoProgressHub = documentoProgressHub;
    }

    @Async("pipelineExecutor")
    public void executar(UUID documentoId, UUID diplomaId) {
        log.info("Job pipeline iniciado documento={}", documentoId);
        try {
            documentoEstadoService.executarPipeline(documentoId, diplomaId);
            log.info("Job pipeline concluído documento={}", documentoId);
        } catch (Exception e) {
            log.error("Job pipeline falhou documento={}: {}", documentoId, e.getMessage(), e);
            persistirFalha(documentoId, e.getMessage());
        }
    }

    @Async("pipelineExecutor")
    public void reprocessar(UUID documentoId, UUID diplomaId) {
        log.info("Job reprocessamento iniciado documento={}", documentoId);
        try {
            documentoEstadoService.reprocessarPipeline(documentoId, diplomaId);
            log.info("Job reprocessamento concluído documento={}", documentoId);
        } catch (Exception e) {
            log.error("Job reprocessamento falhou documento={}: {}", documentoId, e.getMessage(), e);
            persistirFalha(documentoId, e.getMessage());
        }
    }

    /**
     * Grava a mensagem real (não genérica) e distingue FALHA_EXTRACAO de ERRO,
     * para a UI poder orientar o utilizador (ex.: PDF protegido → Imprimir para PDF).
     */
    private void persistirFalha(UUID documentoId, String mensagemOriginal) {
        String msg = (mensagemOriginal != null && !mensagemOriginal.isBlank())
                ? mensagemOriginal
                : "Não foi possível concluir o processamento.";
        if (DocumentoEstadoService.isFalhaExtracao(msg)) {
            documentoEstadoService.marcarFalhaExtracao(documentoId, msg);
            documentoProgressHub.publishError(documentoId, msg, "FALHA_EXTRACAO");
        } else {
            documentoEstadoService.marcarErro(documentoId, msg);
            documentoProgressHub.publishError(documentoId, msg, "ERRO");
        }
    }
}
