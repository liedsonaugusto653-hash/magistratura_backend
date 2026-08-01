package ao.magistratura.dto.simulado;

import ao.magistratura.dto.questao.QuestaoResponse;

import java.util.List;
import java.util.UUID;

public record SimuladoDetailResponse(
        UUID id,
        String titulo,
        String descricao,
        int tempoMinutos,
        UUID categoriaId,
        String categoriaNome,
        List<QuestaoResponse> questoes
) {}
