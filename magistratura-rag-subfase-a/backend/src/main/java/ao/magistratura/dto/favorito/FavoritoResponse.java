package ao.magistratura.dto.favorito;

import java.time.Instant;
import java.util.UUID;

public record FavoritoResponse(
        UUID id,
        UUID artigoId,
        String artigoNumero,
        String artigoTitulo,
        UUID diplomaId,
        String diplomaTitulo,
        Instant dataCriacao
) {}
