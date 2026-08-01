package ao.magistratura.knowledge.retrieval;

import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HybridRetrievalEngine implements RetrievalEngine {

    private final LexicalSearch lexicalSearch;
    private final VectorSearch vectorSearch;
    private final RankingService rankingService;

    @Override
    public KnowledgeResult search(KnowledgeQuery query) {
        int limit = query.limite() > 0 ? query.limite() : 5;
        List<KnowledgePassage> lex = lexicalSearch.search(query);
        List<KnowledgePassage> vec = query.hibrido() ? vectorSearch.search(query) : List.of();
        List<KnowledgePassage> ranked = rankingService.rank(query, vec, lex, limit);

        String estrategia;
        if (!vec.isEmpty() && !lex.isEmpty()) {
            estrategia = "HIBRIDO";
        } else if (!vec.isEmpty()) {
            estrategia = "VECTOR";
        } else if (!lex.isEmpty()) {
            estrategia = "LEXICAL";
        } else {
            estrategia = "NENHUMA";
        }

        return new KnowledgeResult(ranked, estrategia, lex.size() + vec.size(), ranked.isEmpty());
    }
}
