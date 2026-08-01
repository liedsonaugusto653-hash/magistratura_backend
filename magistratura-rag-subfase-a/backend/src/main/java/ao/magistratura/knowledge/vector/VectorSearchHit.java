package ao.magistratura.knowledge.vector;

import java.util.UUID;

public record VectorSearchHit(
        UUID id,
        UUID artigoId,
        UUID diplomaId,
        UUID documentoId,
        String texto,
        double distance,
        String metadadosJson
) {}
