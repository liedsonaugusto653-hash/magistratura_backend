package ao.magistratura.dto.ia;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversaDetailResponse(
        UUID id,
        String titulo,
        Instant dataCriacao,
        Instant dataAtualizacao,
        List<MensagemResponse> mensagens
) {
}
