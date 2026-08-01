package ao.magistratura.pipeline.model;

/**
 * Versão do pipeline documental. Incrementar quando a lógica de extração
 * ou as etapas mudarem de forma relevante.
 */
public final class PipelineVersion {

    /**
     * 2.0.0 — análise de tipo de PDF, OCR (Tesseract) com fallback automático,
     * normalização jurídica, estados de processamento ricos, relatório de qualidade.
     * Extracção continua determinística (sem IA generativa).
     */
    public static final String ATUAL = "2.0.0";

    private PipelineVersion() {
    }
}
