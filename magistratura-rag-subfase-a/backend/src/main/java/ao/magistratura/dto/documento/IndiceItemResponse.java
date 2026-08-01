package ao.magistratura.dto.documento;

import java.util.UUID;

/** Uma entrada do índice navegável de um documento (usada para construir o índice lateral no frontend). */
public record IndiceItemResponse(
        UUID artigoId,
        String capitulo,
        String seccao,
        String numero,
        String titulo,
        Integer ordem,
        Integer paginaInicio
) {}
