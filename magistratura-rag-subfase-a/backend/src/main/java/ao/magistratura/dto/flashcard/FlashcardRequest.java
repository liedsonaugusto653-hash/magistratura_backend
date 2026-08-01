package ao.magistratura.dto.flashcard;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** Criação ou actualização manual de um flashcard. */
public record FlashcardRequest(
        @NotBlank(message = "A pergunta é obrigatória")
        String pergunta,

        @NotBlank(message = "A resposta é obrigatória")
        String resposta,

        UUID diplomaId,
        UUID artigoId,
        UUID temaId,
        UUID categoriaId
) {}
