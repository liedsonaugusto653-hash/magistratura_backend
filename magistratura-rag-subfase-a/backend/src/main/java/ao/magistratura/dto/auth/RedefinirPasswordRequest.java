package ao.magistratura.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirPasswordRequest(
        @NotBlank(message = "O token é obrigatório")
        String token,

        @NotBlank(message = "A nova palavra-passe é obrigatória")
        @Size(min = 8, message = "A palavra-passe deve ter pelo menos 8 caracteres")
        String novaPassword
) {
}
