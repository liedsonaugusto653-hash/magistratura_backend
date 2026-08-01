package ao.magistratura.knowledge.generator;

import ao.magistratura.pipeline.model.KnowledgeChangeSet;

/**
 * Prepara contexto estruturado para o Tutor (ex.: resumos curtos por artigo).
 */
public interface TutorContextGenerator {

    int generateForChanges(KnowledgeChangeSet changes);
}
