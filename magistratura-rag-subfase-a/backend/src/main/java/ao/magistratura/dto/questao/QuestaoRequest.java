package ao.magistratura.dto.questao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/** Criação ou actualização manual de uma questão de escolha múltipla. */
public record QuestaoRequest(
        @NotBlank(message = "O enunciado é obrigatório")
        String enunciado,

        @NotBlank(message = "A opção A é obrigatória")
        String opcaoA,

        @NotBlank(message = "A opção B é obrigatória")
        String opcaoB,

        @NotBlank(message = "A opção C é obrigatória")
        String opcaoC,

        @NotBlank(message = "A opção D é obrigatória")
        String opcaoD,

        @NotBlank(message = "A resposta correta é obrigatória")
        @Pattern(regexp = "[ABCDabcd]", message = "A resposta correta deve ser A, B, C ou D")
        String respostaCorreta,

        String justificacao,

        /** FACIL | MEDIO | DIFICIL — opcional (default MEDIO). */
        String nivelDificuldade,

        UUID diplomaId,
        UUID artigoId,
        UUID temaId,
        UUID categoriaId
) {}
