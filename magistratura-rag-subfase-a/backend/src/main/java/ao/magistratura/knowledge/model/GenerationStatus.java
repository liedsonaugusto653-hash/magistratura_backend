package ao.magistratura.knowledge.model;

/**
 * Ciclo de vida de um artefacto de conhecimento gerado automaticamente.
 */
public enum GenerationStatus {
    PENDENTE,
    GERADO,
    EM_REVISAO,
    APROVADO,
    REJEITADO,
    OBSOLETO
}
