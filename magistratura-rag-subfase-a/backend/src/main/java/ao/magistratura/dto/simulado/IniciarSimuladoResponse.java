package ao.magistratura.dto.simulado;

import ao.magistratura.dto.questao.QuestaoResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IniciarSimuladoResponse(
        UUID tentativaId,
        UUID simuladoId,
        String titulo,
        int tempoMinutos,
        Instant dataInicio,
        List<QuestaoResponse> questoes
) {}
