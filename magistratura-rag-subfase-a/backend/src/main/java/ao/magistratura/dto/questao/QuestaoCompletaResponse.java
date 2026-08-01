package ao.magistratura.dto.questao;

import java.util.UUID;

/** Detalhe completo para edição (inclui gabarito). Não usar na resolução de provas. */
public record QuestaoCompletaResponse(
        UUID id,
        String enunciado,
        String opcaoA,
        String opcaoB,
        String opcaoC,
        String opcaoD,
        String respostaCorreta,
        String justificacao,
        String nivelDificuldade,
        UUID diplomaId,
        String diplomaTitulo
) {}
