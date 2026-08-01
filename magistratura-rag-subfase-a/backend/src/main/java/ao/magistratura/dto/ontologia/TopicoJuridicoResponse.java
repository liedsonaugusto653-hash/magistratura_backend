package ao.magistratura.dto.ontologia;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicoJuridicoResponse(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        UUID entidadeId,
        String entidadeCodigo,
        String entidadeNome,
        UUID parentId,
        String parentNome,
        int ordem,
        int totalArtigos,
        List<RelacaoJuridicaResponse> relacoes,
        String definicaoEstudo,
        List<PerguntaGuiaResponse> perguntasGuia,
        Instant perguntasGuiaGeradoEm,
        String porqueExiste,
        List<String> ondeApareceVida,
        List<String> errosComuns,
        CasoPraticoResponse casoPratico,
        UUID topicoAnteriorId,
        String topicoAnteriorNome,
        UUID topicoSeguinteId,
        String topicoSeguinteNome,
        int posicaoTrilha,
        int totalTrilha
) {
    /** true se a Ficha de Estudo (definição + perguntas-guia) já foi gerada e está em cache. */
    public boolean temFichaEstudo() {
        return definicaoEstudo != null && !definicaoEstudo.isBlank()
                && perguntasGuia != null && !perguntasGuia.isEmpty();
    }
}
