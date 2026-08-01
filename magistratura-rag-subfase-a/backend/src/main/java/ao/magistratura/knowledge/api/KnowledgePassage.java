package ao.magistratura.knowledge.api;

import java.util.UUID;

public record KnowledgePassage(
        UUID id,
        UUID artigoId,
        UUID diplomaId,
        UUID documentoId,
        KnowledgeContentKind kind,
        String diplomaTitulo,
        String diplomaNumero,
        String artigoNumero,
        String artigoTitulo,
        String capitulo,
        String seccao,
        String texto,
        String metodo,
        double score
) {
    /** Título legível para citações / UI. */
    public String titulo() {
        if (artigoTitulo != null && !artigoTitulo.isBlank()) {
            return artigoTitulo;
        }
        if (diplomaTitulo != null && !diplomaTitulo.isBlank()) {
            return diplomaTitulo;
        }
        return "Fonte jurídica";
    }

    /** Referência curta (ex.: Art. 17.º). */
    public String referencia() {
        if (artigoNumero != null && !artigoNumero.isBlank()) {
            String base = artigoNumero.toLowerCase().startsWith("art")
                    ? artigoNumero
                    : "Art. " + artigoNumero;
            if (diplomaNumero != null && !diplomaNumero.isBlank()) {
                return base + " · " + diplomaNumero;
            }
            return base;
        }
        return diplomaNumero != null ? diplomaNumero : "";
    }

    /** Excerpt curto para painel de fontes. */
    public String excerpt() {
        if (texto == null) return null;
        String t = texto.strip();
        return t.length() <= 280 ? t : t.substring(0, 280) + "…";
    }

    public KnowledgePassage comTextoTruncado(String novoTexto) {
        return new KnowledgePassage(
                id, artigoId, diplomaId, documentoId, kind,
                diplomaTitulo, diplomaNumero, artigoNumero, artigoTitulo,
                capitulo, seccao, novoTexto, metodo, score
        );
    }
}
