package ao.magistratura.knowledge.generator;

import ao.magistratura.pipeline.model.KnowledgeChangeSet;

/**
 * Fachada do Knowledge Domain: coordena geradores especializados.
 * Não escreve diretamente em tabelas — delega em FlashcardGenerator, etc.
 */
public interface KnowledgeGenerator {

    /**
     * Processa apenas o change set (incremental).
     * @return total de artefactos tocados (criados/obsoletados)
     */
    int generateIncremental(KnowledgeChangeSet changes);
}
