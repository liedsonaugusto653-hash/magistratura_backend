package ao.magistratura.pipeline.model;

/**
 * Etapas granulares do pipeline documental (Fase 3+).
 * Independentes do {@link ao.magistratura.entity.EstadoDocumento}.
 */
public enum PipelineEtapa {
    RECEBIDO,
    DETECAO_INCREMENTAL,
    VALIDADO,
    ANALISANDO_PDF,
    EXTRAINDO_PDF,
    OCR_EM_EXECUCAO,
    NORMALIZANDO_TEXTO,
    EXTRAINDO_METADADOS,
    EXTRAINDO_ESTRUTURA,
    PERSISTINDO_ARTIGOS,
    INDEXANDO,
    /** Ligação automática artigos ↔ tópicos conceptuais da ontologia. */
    LIGANDO_ONTOLOGIA,
    GERANDO_CONHECIMENTO,
    CONCLUIDO,
    ERRO
}
