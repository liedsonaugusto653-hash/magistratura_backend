package ao.magistratura.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String nome,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "A palavra-passe é obrigatória")
        @Size(min = 8, message = "A palavra-passe deve ter pelo menos 8 caracteres")
        String password
) {
}
