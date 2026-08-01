package ao.magistratura.dto.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Pedido de uma mensagem ao Tutor IA.
 * {@code conversaId} nulo cria automaticamente uma nova conversa.
 * {@code diplomaId}/{@code artigoId}/{@code trecho}/{@code topicoId} são opcionais e
 * ancoram a resposta a um contexto jurídico real (documento ou conceito ontológico).
 */
public record ChatRequest(
        UUID conversaId,

        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 4000, message = "A mensagem não pode exceder 4000 caracteres")
        String mensagem,

        UUID diplomaId,
        UUID artigoId,
        String trecho,
        /** Tópico conceptual da ontologia (estudo por conceito). */
        UUID topicoId
) {
}
