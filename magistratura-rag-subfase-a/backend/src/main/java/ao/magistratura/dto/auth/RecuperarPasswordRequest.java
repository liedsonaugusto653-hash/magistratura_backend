package ao.magistratura.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarPasswordRequest(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Email inválido")
        String email
) {
}
