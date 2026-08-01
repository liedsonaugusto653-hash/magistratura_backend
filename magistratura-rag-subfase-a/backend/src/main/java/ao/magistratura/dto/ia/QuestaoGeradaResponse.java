package ao.magistratura.dto.ia;

import java.util.UUID;

public record QuestaoGeradaResponse(
        /** Preenchido apenas se a questão foi persistida (guardar=true). */
        UUID id,
        String enunciado,
        String opcaoA,
        String opcaoB,
        String opcaoC,
        String opcaoD,
        String respostaCorreta,
        String justificacao
) {
}
