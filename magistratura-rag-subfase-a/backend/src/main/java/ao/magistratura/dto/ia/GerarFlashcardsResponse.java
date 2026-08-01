package ao.magistratura.dto.ia;

import java.util.List;

public record GerarFlashcardsResponse(
        List<FlashcardGeradoResponse> flashcards,
        boolean guardados
) {
}
