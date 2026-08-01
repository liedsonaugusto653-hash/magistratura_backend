package ao.magistratura.knowledge.api;

/**
 * Tipo de conteúdo indexável (distinto de {@link ao.magistratura.knowledge.model.KnowledgeKind},
 * que descreve artefactos gerados: flashcard, questão, etc.).
 */
public enum KnowledgeContentKind {
    LEGISLACAO,
    JURISPRUDENCIA,
    DOUTRINA,
    PARECER,
    GENERICO
}
