package ao.magistratura.dto.ontologia;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LigarArtigoRequest(
        @NotNull UUID artigoId,
        Float relevancia,
        String origemLigacao
) {}
