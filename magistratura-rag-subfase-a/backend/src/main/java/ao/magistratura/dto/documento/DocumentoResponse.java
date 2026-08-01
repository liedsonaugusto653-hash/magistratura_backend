package ao.magistratura.dto.documento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DocumentoResponse(
        UUID id,
        String titulo,
        UUID categoriaId,
        String categoriaNome,
        UUID diplomaId,
        String diplomaTitulo,
        Integer versao,
        String estado,
        /** Rótulo amigável do estado para UI. */
        String estadoRotulo,
        String fonte,
        Boolean oficial,
        LocalDate dataPublicacao,
        Instant dataImportacao,
        Integer numeroPaginas,
        Boolean revisaoNecessaria,
        /** Mensagem curta para o utilizador (sem jargão técnico). */
        String mensagemProgresso,
        Integer progressoPaginasOk,
        Integer progressoPaginasTotal,
        Integer progressoPercentagem,
        /** Resumo legível do resultado (ex.: "322 artigos extraídos"). */
        String resumoResultado,
        /** TEXT / IMAGE / HYBRID / PROTECTED / UNKNOWN */
        String tipoPdf,
        /** Código estável p.ex. PDF_PROTECTED_BLOCKED — null se sem erro estruturado. */
        String codigoErro,
        /** Detalhe persistido (observações de processamento / falha). */
        String observacoesProcessamento,
        /** Passos sugeridos para o utilizador (ex.: Imprimir para PDF). */
        List<String> acoesSugeridas
) {}
