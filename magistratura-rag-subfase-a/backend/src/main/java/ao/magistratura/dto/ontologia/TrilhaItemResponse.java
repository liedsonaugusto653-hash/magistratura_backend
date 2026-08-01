package ao.magistratura.dto.ontologia;

import java.util.UUID;

/** Um passo da trilha de estudo sugerida para uma entidade (ordem calculada a partir de PRESSUPOE). */
public record TrilhaItemResponse(UUID topicoId, String nome, int posicao) {}
