package ao.magistratura.dto.ontologia;

/**
 * Corpo do pedido para gerar (ou regerar) a Ficha de Estudo de um tópico.
 * {@code forcar=true} ignora a cache e pede uma nova geração à IA.
 */
public record GerarFichaEstudoRequest(Boolean forcar) {

    public boolean deveForcar() {
        return Boolean.TRUE.equals(forcar);
    }
}
