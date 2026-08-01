package ao.magistratura.dto.ia;

import java.util.UUID;

public record FlashcardGeradoResponse(
        /** Preenchido apenas se o flashcard foi persistido (guardar=true). */
        UUID id,
        String pergunta,
        String resposta
) {
}
