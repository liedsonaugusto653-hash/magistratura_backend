package ao.magistratura.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarPasswordRequest(
        @NotBlank String passwordAtual,
        @NotBlank @Size(min = 8, max = 100) String novaPassword
) {}
