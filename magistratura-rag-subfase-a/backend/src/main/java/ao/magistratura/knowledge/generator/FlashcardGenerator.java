package ao.magistratura.knowledge.generator;

import ao.magistratura.knowledge.origin.KnowledgeOrigin;
import ao.magistratura.pipeline.model.KnowledgeChangeSet;

/**
 * Gera flashcards apenas para as alterações indicadas no change set.
 */
public interface FlashcardGenerator {

    int generateForChanges(KnowledgeChangeSet changes);

    int obsoleteForOrigin(KnowledgeOrigin origin);
}
