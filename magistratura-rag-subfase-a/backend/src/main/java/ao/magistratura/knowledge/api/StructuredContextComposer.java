package ao.magistratura.knowledge.api;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Compõe contexto RAG por <em>papel jurídico</em>, não só por score:
 * definições, princípios, artigos operativos, relações, jurisprudência.
 * <p>
 * Quando o retrieval devolve sobretudo legislação, classifica heuristicamente
 * pelo texto/título (sem alterar a base de dados).
 */
@Component
public class StructuredContextComposer {

    public enum Slot {
        DEFINICAO,
        PRINCIPIO,
        ARTIGO,
        RELACAO,
        JURISPRUDENCIA
    }

    public record Budget(
            int definicoes,
            int principios,
            int artigos,
            int relacoes,
            int jurisprudencia
    ) {
        public static Budget chatDefault() {
            return new Budget(1, 1, 2, 1, 1);
        }

        public int total() {
            return definicoes + principios + artigos + relacoes + jurisprudencia;
        }
    }

    /**
     * Selecciona passagens diversificadas até preencher o orçamento.
     * Ordem de saída: DEFINICAO → PRINCIPIO → ARTIGO → RELACAO → JURISPRUDENCIA.
     */
    public List<KnowledgePassage> compose(List<KnowledgePassage> ranked, Budget budget) {
        if (ranked == null || ranked.isEmpty()) {
            return List.of();
        }
        Map<Slot, List<KnowledgePassage>> buckets = new LinkedHashMap<>();
        for (Slot s : Slot.values()) {
            buckets.put(s, new ArrayList<>());
        }
        for (KnowledgePassage p : ranked) {
            buckets.get(classify(p)).add(p);
        }

        List<KnowledgePassage> out = new ArrayList<>();
        take(out, buckets.get(Slot.DEFINICAO), budget.definicoes());
        take(out, buckets.get(Slot.PRINCIPIO), budget.principios());
        take(out, buckets.get(Slot.ARTIGO), budget.artigos());
        take(out, buckets.get(Slot.RELACAO), budget.relacoes());
        take(out, buckets.get(Slot.JURISPRUDENCIA), budget.jurisprudencia());

        // Se faltou quota (ex.: sem jurisprudência), preenche com restantes por score original
        if (out.size() < Math.min(budget.total(), ranked.size())) {
            for (KnowledgePassage p : ranked) {
                if (out.size() >= budget.total()) break;
                if (!containsId(out, p)) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private static void take(List<KnowledgePassage> out, List<KnowledgePassage> from, int n) {
        for (int i = 0; i < from.size() && i < n; i++) {
            if (!containsId(out, from.get(i))) {
                out.add(from.get(i));
            }
        }
    }

    private static boolean containsId(List<KnowledgePassage> list, KnowledgePassage p) {
        if (p.id() == null && p.artigoId() == null) {
            return list.stream().anyMatch(x -> x == p);
        }
        return list.stream().anyMatch(x ->
                (p.id() != null && p.id().equals(x.id()))
                        || (p.artigoId() != null && p.artigoId().equals(x.artigoId())));
    }

    public Slot classify(KnowledgePassage p) {
        if (p.kind() == KnowledgeContentKind.JURISPRUDENCIA) {
            return Slot.JURISPRUDENCIA;
        }
        String blob = ((p.artigoTitulo() == null ? "" : p.artigoTitulo()) + " "
                + (p.texto() == null ? "" : p.texto().substring(0, Math.min(400, p.texto().length()))))
                .toLowerCase(Locale.ROOT);

        if (blob.contains("princípio") || blob.contains("principio")
                || blob.contains("legalidade") || blob.contains("proporcionalidade")
                || blob.contains("igualdade") || blob.contains("dignidade")) {
            return Slot.PRINCIPIO;
        }
        if (blob.contains("entende-se por") || blob.contains("considera-se")
                || blob.contains("definição") || blob.contains("definicao")
                || blob.contains("para efeitos") || blob.startsWith("é a")
                || blob.contains("significa")) {
            return Slot.DEFINICAO;
        }
        if (blob.contains("relação") || blob.contains("relacao")
                || blob.contains("sem prejuízo") || blob.contains("nos termos do")
                || blob.contains("em conformidade") || blob.contains("pressupõe")) {
            return Slot.RELACAO;
        }
        return Slot.ARTIGO;
    }
}
