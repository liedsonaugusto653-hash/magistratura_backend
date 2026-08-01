package ao.magistratura.knowledge.generator;

import ao.magistratura.pipeline.model.KnowledgeChangeSet;

public interface SimulationGenerator {

    /**
     * Atualiza pools de simulados apenas com base em questões novas/alteradas.
     * @return número de ligações/simulados tocados
     */
    int generateForChanges(KnowledgeChangeSet changes);
}
