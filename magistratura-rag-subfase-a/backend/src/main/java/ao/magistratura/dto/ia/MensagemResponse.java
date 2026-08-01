package ao.magistratura.dto.ia;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MensagemResponse(
        UUID id,
        UUID conversaId,
        String autor,
        String conteudo,
        UUID diplomaContextoId,
        UUID artigoContextoId,
        Instant timestamp,
        /** Fontes RAG usadas nesta resposta (vazio se não houve retrieval). */
        List<CitacaoFonteResponse> fontes
) {
    public MensagemResponse(
            UUID id,
            UUID conversaId,
            String autor,
            String conteudo,
            UUID diplomaContextoId,
            UUID artigoContextoId,
            Instant timestamp
    ) {
        this(id, conversaId, autor, conteudo, diplomaContextoId, artigoContextoId, timestamp, List.of());
    }
}
