package ao.magistratura.dto.biblioteca;

import java.util.UUID;

public record TemaResponse(
        UUID id,
        String nome,
        String descricao,
        UUID categoriaId,
        String categoriaNome
) {}
