package ao.magistratura.dto.questao;

import java.util.UUID;

public record ResponderQuestaoResponse(
        UUID questaoId,
        String respostaEscolhida,
        String respostaCorreta,
        boolean correta,
        String justificacao
) {}
