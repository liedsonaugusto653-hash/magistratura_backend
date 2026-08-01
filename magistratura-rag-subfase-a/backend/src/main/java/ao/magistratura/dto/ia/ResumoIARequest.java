package ao.magistratura.dto.ia;

import java.util.UUID;

/**
 * Exatamente uma das origens deve ser fornecida: {@code diplomaId},
 * {@code artigoId} ou {@code texto} livre (ex.: trecho selecionado no leitor de PDF).
 */
public record ResumoIARequest(
        UUID diplomaId,
        UUID artigoId,
        String texto
) {
}
