package ao.magistratura.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record UtilizadorResponse(
        UUID id,
        String nome,
        String email,
        String fotografiaUrl,
        Instant dataCriacao,
        Instant ultimoLogin,
        String preferenciasJson
) {}
