package ao.magistratura.service.pdf;

/**
 * Resultado da extração estrutural de um artigo a partir do PDF, ainda sem
 * ligação a entidades JPA — o {@code DocumentoService} converte isto em
 * {@link ao.magistratura.entity.Artigo}.
 */
public record ArtigoExtraido(
        String numero,
        String titulo,
        String texto,
        Integer ordem,
        String capitulo,
        String seccao,
        Integer paginaInicio,
        Integer paginaFim
) {
}
