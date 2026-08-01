package ao.magistratura.dto.ia;

import java.util.List;

public record GerarQuestoesResponse(
        List<QuestaoGeradaResponse> questoes,
        boolean guardados
) {
}
