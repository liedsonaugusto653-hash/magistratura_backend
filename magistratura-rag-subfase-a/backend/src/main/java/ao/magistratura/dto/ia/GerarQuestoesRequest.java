package ao.magistratura.dto.ia;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record GerarQuestoesRequest(
        UUID diplomaId,
        UUID artigoId,

        @Min(value = 1, message = "A quantidade minima e 1")
        @Max(value = 3, message = "A quantidade maxima e 3 por pedido (IA local)")
        Integer quantidade,

        /** Se verdadeiro, as questoes geradas sao imediatamente persistidas no banco de questoes. */
        boolean guardar
) {
    public int quantidadeOuDefeito() {
        int q = quantidade == null ? 2 : quantidade;
        return Math.min(Math.max(q, 1), 3);
    }
}
