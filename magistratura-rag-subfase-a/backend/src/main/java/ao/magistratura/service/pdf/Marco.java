package ao.magistratura.service.pdf;

/**
 * Um "marco" é qualquer ponto de interesse estrutural encontrado no texto
 * bruto do PDF: o início de um capítulo, de uma secção ou de um artigo.
 * {@code posicao} é o offset (em caracteres) no texto completo concatenado,
 * usado pelo {@link EstruturaJuridicaParser} para calcular onde cada bloco
 * de texto começa e acaba, e a que página pertence.
 */
public record Marco(TipoMarco tipo, int posicao, String rotulo, String titulo) {

    public enum TipoMarco {
        CAPITULO,
        SECCAO,
        ARTIGO
    }
}
