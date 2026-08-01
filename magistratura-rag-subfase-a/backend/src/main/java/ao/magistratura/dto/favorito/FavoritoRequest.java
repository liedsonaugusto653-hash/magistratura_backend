package ao.magistratura.dto.favorito;

import java.util.UUID;

/**
 * Pelo menos um de {@code artigoId} ou {@code diplomaId} deve ser fornecido.
 */
public record FavoritoRequest(
        UUID artigoId,
        UUID diplomaId
) {}
