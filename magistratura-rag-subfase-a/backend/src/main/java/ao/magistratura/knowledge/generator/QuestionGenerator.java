package ao.magistratura.knowledge.generator;

import ao.magistratura.knowledge.origin.KnowledgeOrigin;
import ao.magistratura.pipeline.model.KnowledgeChangeSet;

public interface QuestionGenerator {

    int generateForChanges(KnowledgeChangeSet changes);

    int obsoleteForOrigin(KnowledgeOrigin origin);
}
