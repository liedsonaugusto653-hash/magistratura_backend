package ao.magistratura.dto.biblioteca;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/** Dados necessários para criar um novo diploma na Biblioteca Jurídica. */
public record DiplomaRequest(
        @NotBlank(message = "O número do diploma é obrigatório")
        String numero,

        @NotBlank(message = "O título do diploma é obrigatório")
        String titulo,

        String descricao,

        LocalDate dataPublicacao,

        UUID categoriaId
) {
}
