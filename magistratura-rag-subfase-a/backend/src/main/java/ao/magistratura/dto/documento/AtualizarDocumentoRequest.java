package ao.magistratura.dto.documento;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Atualização parcial dos metadados de um {@link ao.magistratura.entity.Documento}.
 * Todos os campos são opcionais: só os que vierem preenchidos são alterados.
 * Não permite mudar o ficheiro, o diploma ou o estado — isso continua a
 * fazer-se via /processar e /reprocessar.
 */
public record AtualizarDocumentoRequest(
        String titulo,
        UUID categoriaId,
        String fonte,
        Boolean oficial,
        LocalDate dataPublicacao
) {}