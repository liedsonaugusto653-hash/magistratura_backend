package ao.magistratura.dto.ontologia;

import java.util.UUID;

public record SugestaoLigacaoResponse(
        UUID artigoId,
        String artigoNumero,
        String artigoTitulo,
        UUID topicoId,
        String topicoCodigo,
        String topicoNome,
        float score,
        boolean jaExistia,
        /** SUGESTAO | CRIADA | JA_EXISTIA */
        String estado
) {}
