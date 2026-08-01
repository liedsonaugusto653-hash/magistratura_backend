package ao.magistratura.service.pdf;

/**
 * Texto extraído de uma única página de um PDF, com o respetivo número
 * de página (1-indexado), para que a origem de cada artigo/trecho seja
 * sempre rastreável até à página exata do documento oficial.
 */
public record PaginaTexto(int numeroPagina, String texto) {
}
