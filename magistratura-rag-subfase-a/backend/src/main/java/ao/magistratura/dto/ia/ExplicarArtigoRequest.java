package ao.magistratura.dto.ia;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ExplicarArtigoRequest(
        @NotNull(message = "O artigo é obrigatório")
        UUID artigoId,

        /** Trecho específico selecionado pelo estudante dentro do artigo (opcional). */
        String trecho
) {
}
