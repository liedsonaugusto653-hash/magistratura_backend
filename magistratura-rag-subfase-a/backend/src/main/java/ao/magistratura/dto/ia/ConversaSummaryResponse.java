package ao.magistratura.dto.ia;

import java.time.Instant;
import java.util.UUID;

public record ConversaSummaryResponse(
        UUID id,
        String titulo,
        Instant dataCriacao,
        Instant dataAtualizacao
) {
}
