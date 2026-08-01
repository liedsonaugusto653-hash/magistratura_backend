package ao.magistratura.knowledge.generator;

import ao.magistratura.knowledge.origin.KnowledgeOrigin;
import ao.magistratura.pipeline.model.KnowledgeChangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementações no-op até a Fase 4 ativar geração real.
 * Mantém o grafo de dependências e permite testes do orquestrador.
 */
public final class NoOpKnowledgeGenerators {

    private NoOpKnowledgeGenerators() {
    }

    @Component
    public static class NoOpFlashcardGenerator implements FlashcardGenerator {
        private static final Logger log = LoggerFactory.getLogger(NoOpFlashcardGenerator.class);

        @Override
        public int generateForChanges(KnowledgeChangeSet changes) {
            log.debug("NoOpFlashcardGenerator: {} novos, {} alterados",
                    changes.getArtigosNovosIds().size(), changes.getArtigosAlteradosIds().size());
            return 0;
        }

        @Override
        public int obsoleteForOrigin(KnowledgeOrigin origin) {
            return 0;
        }
    }

    @Component
    public static class NoOpQuestionGenerator implements QuestionGenerator {
        private static final Logger log = LoggerFactory.getLogger(NoOpQuestionGenerator.class);

        @Override
        public int generateForChanges(KnowledgeChangeSet changes) {
            log.debug("NoOpQuestionGenerator: changeSet size={}",
                    changes.getArtigosNovosIds().size() + changes.getArtigosAlteradosIds().size());
            return 0;
        }

        @Override
        public int obsoleteForOrigin(KnowledgeOrigin origin) {
            return 0;
        }
    }

    @Component
    public static class NoOpSimulationGenerator implements SimulationGenerator {
        @Override
        public int generateForChanges(KnowledgeChangeSet changes) {
            return 0;
        }
    }

    @Component
    public static class NoOpTutorContextGenerator implements TutorContextGenerator {
        @Override
        public int generateForChanges(KnowledgeChangeSet changes) {
            return 0;
        }
    }

    @Component
    public static class NoOpEmbeddingGenerator implements EmbeddingGenerator {
        @Override
        public int generateForChanges(KnowledgeChangeSet changes) {
            return 0;
        }
    }

    @Component
    public static class DefaultKnowledgeGenerator implements KnowledgeGenerator {
        private static final Logger log = LoggerFactory.getLogger(DefaultKnowledgeGenerator.class);

        private final FlashcardGenerator flashcardGenerator;
        private final QuestionGenerator questionGenerator;
        private final SimulationGenerator simulationGenerator;
        private final TutorContextGenerator tutorContextGenerator;
        private final EmbeddingGenerator embeddingGenerator;

        public DefaultKnowledgeGenerator(
                FlashcardGenerator flashcardGenerator,
                QuestionGenerator questionGenerator,
                SimulationGenerator simulationGenerator,
                TutorContextGenerator tutorContextGenerator,
                EmbeddingGenerator embeddingGenerator
        ) {
            this.flashcardGenerator = flashcardGenerator;
            this.questionGenerator = questionGenerator;
            this.simulationGenerator = simulationGenerator;
            this.tutorContextGenerator = tutorContextGenerator;
            this.embeddingGenerator = embeddingGenerator;
        }

        @Override
        public int generateIncremental(KnowledgeChangeSet changes) {
            if (changes == null || changes.isEmpty()) {
                log.debug("KnowledgeGenerator: change set vazio — nada a fazer");
                return 0;
            }
            int total = 0;
            total += embeddingGenerator.generateForChanges(changes);
            total += flashcardGenerator.generateForChanges(changes);
            total += questionGenerator.generateForChanges(changes);
            total += simulationGenerator.generateForChanges(changes);
            total += tutorContextGenerator.generateForChanges(changes);
            log.info("KnowledgeGenerator: {} artefactos tocados (ainda no-op na Fase 3)", total);
            return total;
        }
    }
}
