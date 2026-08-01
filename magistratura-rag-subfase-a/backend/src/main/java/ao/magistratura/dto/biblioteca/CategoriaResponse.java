package ao.magistratura.dto.biblioteca;

import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        String nome,
        String descricao
) {}
