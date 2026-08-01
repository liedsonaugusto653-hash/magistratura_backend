package ao.magistratura.knowledge.ranking;

import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RankingService {

    private static final int RRF_K = 60;

    public List<KnowledgePassage> rank(
            KnowledgeQuery query,
            List<KnowledgePassage> vectorHits,
            List<KnowledgePassage> lexicalHits,
            int limite) {

        Map<UUID, Double> scores = new LinkedHashMap<>();
        Map<UUID, KnowledgePassage> best = new HashMap<>();

        accumulateRrf(vectorHits, scores, best);
        accumulateRrf(lexicalHits, scores, best);

        for (Map.Entry<UUID, Double> e : scores.entrySet()) {
            KnowledgePassage p = best.get(e.getKey());
            double s = e.getValue();
            if (query.artigoId() != null && query.artigoId().equals(p.artigoId())) {
                s += 2.0;
            }
            if (query.diplomaId() != null && query.diplomaId().equals(p.diplomaId())) {
                s += 0.5;
            }
            if ("NUMERO".equals(p.metodo()) || "ID".equals(p.metodo()) || "NUMERO_DIPLOMA".equals(p.metodo())
                    || "ID_EXPLICITO".equals(p.metodo()) || "ID_ARTIGO".equals(p.metodo())) {
                s += 1.0;
            }
            e.setValue(s);
        }

        int lim = limite > 0 ? limite : 5;
        return scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(lim)
                .map(e -> {
                    KnowledgePassage p = best.get(e.getKey());
                    return new KnowledgePassage(
                            p.id(), p.artigoId(), p.diplomaId(), p.documentoId(), p.kind(),
                            p.diplomaTitulo(), p.diplomaNumero(), p.artigoNumero(), p.artigoTitulo(),
                            p.capitulo(), p.seccao(), p.texto(), "FUSION", e.getValue());
                })
                .toList();
    }

    private void accumulateRrf(List<KnowledgePassage> list,
                               Map<UUID, Double> scores,
                               Map<UUID, KnowledgePassage> best) {
        if (list == null) {
            return;
        }
        int rank = 1;
        for (KnowledgePassage p : list) {
            UUID key = p.artigoId() != null ? p.artigoId() : p.id();
            if (key == null) {
                continue;
            }
            best.merge(key, p, (a, b) -> {
                int la = a.texto() != null ? a.texto().length() : 0;
                int lb = b.texto() != null ? b.texto().length() : 0;
                return la >= lb ? a : b;
            });
            scores.merge(key, 1.0 / (RRF_K + rank), Double::sum);
            rank++;
        }
    }
}
