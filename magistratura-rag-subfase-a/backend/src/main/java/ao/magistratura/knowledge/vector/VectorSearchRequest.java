package ao.magistratura.knowledge.vector;

import ao.magistratura.knowledge.api.KnowledgeContentKind;

import java.util.UUID;

public record VectorSearchRequest(
        float[] query,
        UUID diplomaId,
        KnowledgeContentKind kind,
        String modeloEmbedding,
        int limite
) {}
