package ao.magistratura.dto.ontologia;

import java.util.UUID;

/**
 * Pedido de auto-ligação ontológica.
 * Indicar {@code documentoId} ou {@code diplomaId} (pelo menos um).
 * {@code dryRun=true} devolve sugestões sem gravar.
 */
public record AutoLigarRequest(
        UUID documentoId,
        UUID diplomaId,
        Boolean dryRun
) {}
