package ao.magistratura.dto.ia;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record GerarFlashcardsRequest(
        UUID diplomaId,
        UUID artigoId,

        @Min(value = 1, message = "A quantidade minima e 1")
        @Max(value = 5, message = "A quantidade maxima e 5 por pedido (IA local)")
        Integer quantidade,

        /** Se verdadeiro, os flashcards gerados sao imediatamente persistidos na biblioteca. */
        boolean guardar
) {
    public int quantidadeOuDefeito() {
        int q = quantidade == null ? 3 : quantidade;
        return Math.min(Math.max(q, 1), 5);
    }
}
