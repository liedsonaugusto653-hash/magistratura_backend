package ao.magistratura.dto.biblioteca;

import java.time.LocalDate;
import java.util.UUID;

/** Resumo para listagens — sem artigos. */
public record DiplomaResumoResponse(
        UUID id,
        String numero,
        String titulo,
        String descricao,
        LocalDate dataPublicacao,
        String estado,
        UUID categoriaId,
        String categoriaNome
) {}
