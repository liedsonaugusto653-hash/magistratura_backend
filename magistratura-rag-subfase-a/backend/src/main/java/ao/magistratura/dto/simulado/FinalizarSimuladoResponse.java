package ao.magistratura.dto.simulado;

import java.time.Instant;
import java.util.UUID;

public record FinalizarSimuladoResponse(
        UUID tentativaId,
        UUID simuladoId,
        Instant dataInicio,
        Instant dataFim,
        int totalQuestoes,
        int acertos,
        int erros,
        double pontuacao,
        boolean concluido
) {}
