package ao.magistratura.knowledge.api;

import java.util.List;

public record KnowledgeResult(
        List<KnowledgePassage> passagens,
        String estrategia,
        int totalCandidatos,
        boolean vazio
) {
    public static KnowledgeResult vazioResultado() {
        return new KnowledgeResult(List.of(), "NENHUMA", 0, true);
    }
}