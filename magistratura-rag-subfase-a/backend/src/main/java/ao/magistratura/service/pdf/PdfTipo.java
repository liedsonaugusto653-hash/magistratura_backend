package ao.magistratura.service.pdf;

/**
 * Classificação do conteúdo textual de um PDF jurídico.
 */
public enum PdfTipo {
    /** Camada de texto suficiente para extracção directa (PDFBox). */
    TEXT,
    /** Pouco ou nenhum texto seleccionável — tipicamente scan/imagem. */
    IMAGE,
    /** Mistura: algumas páginas com texto, outras só imagem. */
    HYBRID,
    /** PDF encriptado ou ilegível sem password/permissões. */
    PROTECTED,
    /** Não foi possível classificar. */
    UNKNOWN
}
