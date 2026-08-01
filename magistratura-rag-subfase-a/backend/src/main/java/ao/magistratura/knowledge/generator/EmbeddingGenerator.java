package ao.magistratura.knowledge.generator;

import ao.magistratura.pipeline.model.KnowledgeChangeSet;

/**
 * Gera embeddings apenas para artigos no change set.
 * Pode delegar no {@link ao.magistratura.pipeline.index.KnowledgeIndexer}.
 */
public interface EmbeddingGenerator {

    int generateForChanges(KnowledgeChangeSet changes);
}
