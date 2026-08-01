package ao.magistratura.knowledge.api;

import java.util.UUID;

/**
 * Consulta à Knowledge Layer.
 * {@code topicoId} (Fase 3 ontologia) ancora a pesquisa a artigos ligados
 * a um tópico conceptual e permite estudo por conceito.
 */
public record KnowledgeQuery(
        String texto,
        UUID diplomaId,
        UUID artigoId,
        UUID topicoId,
        KnowledgeContentKind kind,
        int limite,
        boolean hibrido
) {
    public static KnowledgeQuery of(String texto, int limite) {
        return new KnowledgeQuery(texto, null, null, null, KnowledgeContentKind.LEGISLACAO, limite, true);
    }

    public static KnowledgeQuery juridico(String texto, UUID diplomaId, UUID artigoId, int limite) {
        return new KnowledgeQuery(texto, diplomaId, artigoId, null, KnowledgeContentKind.LEGISLACAO, limite, true);
    }

    /** Pesquisa ancorada a um tópico conceptual da ontologia. */
    public static KnowledgeQuery porTopico(String texto, UUID topicoId, int limite) {
        return new KnowledgeQuery(texto, null, null, topicoId, KnowledgeContentKind.LEGISLACAO, limite, true);
    }

    public static KnowledgeQuery juridicoComTopico(
            String texto, UUID diplomaId, UUID artigoId, UUID topicoId, int limite) {
        return new KnowledgeQuery(texto, diplomaId, artigoId, topicoId, KnowledgeContentKind.LEGISLACAO, limite, true);
    }
}
