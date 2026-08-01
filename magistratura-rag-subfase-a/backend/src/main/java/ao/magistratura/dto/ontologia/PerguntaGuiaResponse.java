package ao.magistratura.dto.ontologia;

/**
 * Uma pergunta-guia da Ficha de Estudo de um tópico conceptual (ex.: "Quem?",
 * "Pode fazer o quê?", "Em que condições?"), com a resposta ancorada nos
 * artigos ligados ao tópico.
 */
public record PerguntaGuiaResponse(String pergunta, String resposta) {}
