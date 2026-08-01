package ao.magistratura.dto.biblioteca;

import java.util.UUID;

/** Resumo para listagens — sem o texto completo. */
public record ArtigoResumoResponse(
        UUID id,
        String numero,
        String titulo,
        Integer ordem,
        UUID diplomaId,
        String diplomaTitulo,
        UUID temaId,
        String temaNome,
        String capitulo,
        String seccao
) {}
