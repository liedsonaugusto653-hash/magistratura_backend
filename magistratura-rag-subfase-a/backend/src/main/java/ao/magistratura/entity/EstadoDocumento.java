package ao.magistratura.entity;

/**
 * Ciclo de vida de um documento PDF na Biblioteca Jurídica.
 * <p>
 * Estados legados ({@code IMPORTADO}, {@code PROCESSANDO}, {@code PROCESSADO}, {@code ERRO})
 * mantêm-se para compatibilidade com o frontend. Os estados intermédios e
 * {@code PROCESSADO_COM_AVISOS} / {@code FALHA_EXTRACAO} permitem telemetria fina
 * sem partir clientes que apenas olham para PROCESSADO vs ERRO.
 */
public enum EstadoDocumento {
    IMPORTADO,
    ANALISANDO,
    EXTRAINDO_TEXTO,
    OCR_EM_EXECUCAO,
    ESTRUTURANDO,
    /** Processamento em curso (legado / genérico). */
    PROCESSANDO,
    /** Concluído com artigos extraídos e sem avisos de qualidade graves. */
    PROCESSADO,
    /** Concluído mas com avisos (OCR, revisão sugerida, baixa confiança). */
    PROCESSADO_COM_AVISOS,
    /** Extracção de texto falhou (scan sem OCR, PDF protegido, etc.). */
    FALHA_EXTRACAO,
    ERRO
}
