package ao.magistratura.dto.ontologia;

import java.util.UUID;

public record EntidadeJuridicaResponse(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        String icone,
        int ordem,
        int totalTopicos
) {}
