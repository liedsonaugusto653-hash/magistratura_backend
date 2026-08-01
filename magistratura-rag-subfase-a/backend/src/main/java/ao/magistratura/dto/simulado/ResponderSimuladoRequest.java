package ao.magistratura.dto.simulado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ResponderSimuladoRequest(
        @NotNull UUID questaoId,
        @NotBlank @Pattern(regexp = "[ABCDabcd]") String resposta
) {}
