package ao.magistratura.dto.ontologia;

import java.util.UUID;

public record TopicoArtigoResponse(
        UUID ligacaoId,
        UUID artigoId,
        String artigoNumero,
        String artigoTitulo,
        UUID diplomaId,
        String diplomaNumero,
        String diplomaTitulo,
        float relevancia,
        String origemLigacao
) {}
