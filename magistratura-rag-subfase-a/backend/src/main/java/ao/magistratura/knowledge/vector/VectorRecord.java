package ao.magistratura.knowledge.vector;

import ao.magistratura.knowledge.api.KnowledgeContentKind;

import java.util.UUID;

public record VectorRecord(
        UUID id,
        UUID artigoId,
        UUID diplomaId,
        UUID documentoId,
        KnowledgeContentKind kind,
        String texto,
        float[] embedding,
        String modeloEmbedding,
        String metadadosJson
) {}
