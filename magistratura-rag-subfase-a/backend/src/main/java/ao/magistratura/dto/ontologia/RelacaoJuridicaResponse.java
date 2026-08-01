package ao.magistratura.dto.ontologia;

import java.util.UUID;

public record RelacaoJuridicaResponse(
        UUID id,
        String tipoRelacao,
        float peso,
        String notas,
        UUID topicoId,
        String topicoCodigo,
        String topicoNome,
        /** true se este tópico é a origem da relação */
        boolean outgoing
) {}
