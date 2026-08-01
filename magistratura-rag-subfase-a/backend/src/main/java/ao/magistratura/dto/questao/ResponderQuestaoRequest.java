package ao.magistratura.dto.questao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResponderQuestaoRequest(
        @NotBlank(message = "A resposta é obrigatória")
        @Pattern(regexp = "[ABCDabcd]", message = "A resposta deve ser A, B, C ou D")
        String resposta
) {}
