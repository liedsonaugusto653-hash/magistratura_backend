package ao.magistratura.dto.simulado;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GerarSimuladoRequest(
        @NotBlank @Size(max = 250) String titulo,
        @Size(max = 2000) String descricao,
        UUID diplomaId,
        UUID artigoId,
        String assunto,
        String dificuldade,
        @Min(1) @Max(15) Integer quantidadeQuestoes,
        @Min(5) @Max(300) Integer tempoMinutos
) {
    public int quantidadeOuDefeito() {
        int q = quantidadeQuestoes == null ? 5 : quantidadeQuestoes;
        // Modelos locais lidam melhor com pedidos pequenos
        return Math.min(Math.max(q, 1), 15);
    }

    public int tempoOuDefeito() {
        return tempoMinutos == null ? 60 : tempoMinutos;
    }

    public String assuntoOuVazio() {
        return assunto != null ? assunto.trim() : "";
    }
}
