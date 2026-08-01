package ao.magistratura.dto.flashcard;

import java.util.UUID;

public record FlashcardResponse(
        UUID id,
        String pergunta,
        String resposta,
        UUID temaId,
        String temaNome,
        UUID categoriaId,
        String categoriaNome,
        UUID diplomaId,
        String diplomaTitulo,
        ProgressoResumo progresso
) {
    public record ProgressoResumo(
            int vezesRevisto,
            int acertos,
            int erros,
            double percentagemAcerto,
            String nivelDificuldade
    ) {}
}
