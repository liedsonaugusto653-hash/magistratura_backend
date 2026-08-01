package ao.magistratura.dto.flashcard;

import jakarta.validation.constraints.NotNull;

public record FlashcardRevisarRequest(
        @NotNull(message = "O campo 'acertou' é obrigatório")
        Boolean acertou
) {}
