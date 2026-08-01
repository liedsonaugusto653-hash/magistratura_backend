package ao.magistratura.dto.ia;

import java.util.UUID;

/**
 * Fonte jurídica recuperada pelo RAG, numerada para citação interativa no chat.
 * O modelo deve referir-se a esta fonte como {@code [n]} no texto da resposta.
 */
public record CitacaoFonteResponse(
        int n,
        UUID artigoId,
        UUID diplomaId,
        String diplomaTitulo,
        String diplomaNumero,
        String artigoNumero,
        String artigoTitulo,
        String capitulo,
        String seccao,
        /** Excerto (truncado) para pré-visualização no painel de fontes. */
        String extrato,
        String metodo,
        double score
) {
}
