package ao.magistratura.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequest(
        @NotBlank @Size(max = 150) String nome,
        @Email @Size(max = 180) String email,
        @Size(max = 500) String fotografiaUrl
) {}
