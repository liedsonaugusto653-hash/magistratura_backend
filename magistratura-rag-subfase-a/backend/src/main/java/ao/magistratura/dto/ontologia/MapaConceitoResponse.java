package ao.magistratura.dto.ontologia;

import java.util.List;

/**
 * Vista agregada para o ecrã "estudar por conceito":
 * entidade → tópicos → artigos ligados + relações.
 */
public record MapaConceitoResponse(
        EntidadeJuridicaResponse entidade,
        List<TopicoJuridicoResponse> topicos,
        List<TopicoArtigoResponse> artigos
) {}
