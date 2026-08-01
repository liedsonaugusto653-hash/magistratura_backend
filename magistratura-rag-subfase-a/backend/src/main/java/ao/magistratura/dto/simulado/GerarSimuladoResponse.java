package ao.magistratura.dto.simulado;

import java.util.UUID;

public record GerarSimuladoResponse(
        UUID simuladoId,
        String titulo,
        int quantidadeQuestoes,
        int tempoMinutos,
        String origemConhecimento
) {}
