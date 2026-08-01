package ao.magistratura.knowledge.ranking;

import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RankingServiceTest {

    private final RankingService ranking = new RankingService();

    @Test
    void matchArtigoExplicito_sobeNoRanking() {
        UUID alvo = UUID.randomUUID();
        UUID outro = UUID.randomUUID();
        KnowledgePassage p1 = passage(outro, "VECTOR", 0.9);
        KnowledgePassage p2 = passage(alvo, "LEXICAL", 0.5);
        KnowledgeQuery q = new KnowledgeQuery("prisão preventiva", null, alvo, null,
                KnowledgeContentKind.LEGISLACAO, 5, true);

        List<KnowledgePassage> ranked = ranking.rank(q, List.of(p1), List.of(p2), 5);
        assertFalse(ranked.isEmpty());
        assertEquals(alvo, ranked.get(0).artigoId());
        assertEquals("FUSION", ranked.get(0).metodo());
    }

    @Test
    void hibrido_combinaListas() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<KnowledgePassage> ranked = ranking.rank(
                KnowledgeQuery.of("teste", 5),
                List.of(passage(a, "VECTOR", 0.8)),
                List.of(passage(b, "NUMERO", 0.7)),
                5);
        assertEquals(2, ranked.size());
    }

    private static KnowledgePassage passage(UUID artigoId, String metodo, double score) {
        return new KnowledgePassage(
                artigoId, artigoId, UUID.randomUUID(), null,
                KnowledgeContentKind.LEGISLACAO,
                "CRA", "CRA/2010", "1", "Título", null, null,
                "texto do artigo", metodo, score);
    }
}
