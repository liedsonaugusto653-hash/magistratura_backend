package ao.magistratura.dto.flashcard;

import java.util.UUID;

public record FlashcardRevisarResponse(
        UUID flashcardId,
        boolean acertou,
        int vezesRevisto,
        int acertos,
        int erros,
        double percentagemAcerto,
        String nivelDificuldade
) {}
