package ao.magistratura.dto.simulado;

import java.util.UUID;

public record SimuladoResumoResponse(
        UUID id,
        String titulo,
        String descricao,
        int tempoMinutos,
        int totalQuestoes,
        UUID categoriaId,
        String categoriaNome
) {}
