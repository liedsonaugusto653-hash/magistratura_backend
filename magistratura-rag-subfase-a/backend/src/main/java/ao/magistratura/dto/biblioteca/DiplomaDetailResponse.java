package ao.magistratura.dto.biblioteca;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detalhe completo de um diploma, incluindo lista resumida de artigos. */
public record DiplomaDetailResponse(
        UUID id,
        String numero,
        String titulo,
        String descricao,
        LocalDate dataPublicacao,
        String estado,
        String resumo,
        String pdfUrl,
        Integer versao,
        Instant dataCriacao,
        UUID categoriaId,
        String categoriaNome,
        List<ArtigoResumoResponse> artigos
) {}
