package ao.magistratura.dto.biblioteca;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detalhe completo de um artigo, incluindo o texto oficial extraído do PDF
 * e metadados de origem documental. Campos novos são opcionais (null)
 * para preservar compatibilidade com clientes que só usam o núcleo antigo.
 */
public record ArtigoDetailResponse(
        UUID id,
        String numero,
        String titulo,
        String texto,
        Integer ordem,
        String resumo,
        Integer paginaInicio,
        Integer paginaFim,
        UUID diplomaId,
        String diplomaTitulo,
        String diplomaNumero,
        UUID temaId,
        String temaNome,
        // --- estrutura jurídica ---
        String capitulo,
        String seccao,
        // --- diploma / versão (preparado para multi-versão) ---
        String diplomaEstado,
        LocalDate diplomaDataPublicacao,
        Integer diplomaVersao,
        UUID diplomaCategoriaId,
        String diplomaCategoriaNome,
        // --- documento de origem (PDF) ---
        UUID documentoId,
        String documentoTitulo,
        String documentoFonte,
        Boolean documentoOficial,
        Instant documentoDataImportacao,
        // --- navegação no mesmo diploma ---
        UUID artigoAnteriorId,
        UUID artigoSeguinteId
) {}
