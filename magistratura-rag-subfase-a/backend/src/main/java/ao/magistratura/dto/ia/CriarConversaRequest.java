package ao.magistratura.dto.ia;

import jakarta.validation.constraints.Size;

public record CriarConversaRequest(
        @Size(max = 250, message = "O título não pode exceder 250 caracteres")
        String titulo
) {
}
