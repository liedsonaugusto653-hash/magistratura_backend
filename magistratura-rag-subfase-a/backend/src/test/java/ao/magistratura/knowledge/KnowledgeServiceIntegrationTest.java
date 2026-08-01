package ao.magistratura.knowledge;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;
import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.chunk.LegalChunker;
import ao.magistratura.knowledge.embedding.EmbeddingProvider;
import ao.magistratura.knowledge.ranking.RankingService;
import ao.magistratura.knowledge.vector.VectorRecord;
import ao.magistratura.knowledge.vector.VectorSearchHit;
import ao.magistratura.knowledge.vector.VectorSearchRequest;
import ao.magistratura.knowledge.vector.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração sem Spring/Postgres/Ollama:
 * Chunker → Embedding determinístico → VectorStore memória → Ranking.
 * Prova o pipeline de conhecimento ponta-a-ponta ao nível da layer.
 */
class KnowledgeServiceIntegrationTest {

    private InMemoryVectorStore vectorStore;
    private EmbeddingProvider emb;
    private RankingService ranking;
    private LegalChunker chunker;
    private UUID diplomaId;
    private UUID artigoId;
    private Artigo artigo;

    @BeforeEach
    void setUp() {
        diplomaId = UUID.randomUUID();
        artigoId = UUID.randomUUID();
        Diploma diploma = Diploma.builder()
                .id(diplomaId)
                .titulo("Constituição da República de Angola")
                .numero("CRA/2010")
                .build();
        artigo = Artigo.builder()
                .id(artigoId)
                .numero("21")
                .titulo("Direitos fundamentais")
                .texto("O Estado respeita e garante os direitos fundamentais de todos os cidadãos.")
                .ordem(21)
                .diploma(diploma)
                .build();

        vectorStore = new InMemoryVectorStore();
        emb = new DeterministicEmbeddingProvider(32);
        ranking = new RankingService();
        chunker = new LegalChunker();

        // Indexar chunks
        for (LegalChunker.Chunk c : chunker.chunkArtigo(artigo)) {
            float[] v = emb.embed("search_document: " + c.texto());
            vectorStore.upsert(new VectorRecord(
                    c.id(), artigoId, diplomaId, null,
                    KnowledgeContentKind.LEGISLACAO, c.texto(), v, emb.modelo(), "{}"));
        }
        assertFalse(vectorStore.records.isEmpty());
    }

    @Test
    void vectorSearch_encontraArtigoPorSemelhanca() {
        float[] q = emb.embed("search_query: direitos fundamentais cidadãos");
        List<VectorSearchHit> hits = vectorStore.search(new VectorSearchRequest(
                q, diplomaId, KnowledgeContentKind.LEGISLACAO, emb.modelo(), 5));
        assertFalse(hits.isEmpty());
        assertEquals(artigoId, hits.get(0).artigoId());
    }

    @Test
    void ranking_combinaLexicalEVector() {
        KnowledgePassage vec = new KnowledgePassage(
                UUID.randomUUID(), artigoId, diplomaId, null, KnowledgeContentKind.LEGISLACAO,
                "CRA", "CRA/2010", "21", "Direitos", null, null,
                artigo.getTexto(), "VECTOR", 0.9);
        KnowledgePassage lex = new KnowledgePassage(
                UUID.randomUUID(), artigoId, diplomaId, null, KnowledgeContentKind.LEGISLACAO,
                "CRA", "CRA/2010", "21", "Direitos", null, null,
                artigo.getTexto(), "LEXICAL", 0.6);

        List<KnowledgePassage> ranked = ranking.rank(
                KnowledgeQuery.of("direitos", 3), List.of(vec), List.of(lex), 3);
        assertEquals(1, ranked.size()); // mesmo artigoId → fundido
        assertEquals("FUSION", ranked.get(0).metodo());
        assertEquals(artigoId, ranked.get(0).artigoId());
    }

    @Test
    void filtroDiploma_excluiOutros() {
        UUID outro = UUID.randomUUID();
        float[] q = emb.embed("search_query: direitos");
        List<VectorSearchHit> hits = vectorStore.search(new VectorSearchRequest(
                q, outro, KnowledgeContentKind.LEGISLACAO, emb.modelo(), 5));
        assertTrue(hits.isEmpty());
    }

    @Test
    void chunker_preservaNumeroArtigo() {
        List<LegalChunker.Chunk> chunks = chunker.chunkArtigo(artigo);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).texto().contains("Artigo 21"));
    }

    // --- helpers ---

    static class DeterministicEmbeddingProvider implements EmbeddingProvider {
        private final int dim;
        DeterministicEmbeddingProvider(int dim) { this.dim = dim; }
        @Override public String nome() { return "test"; }
        @Override public String modelo() { return "test-embed"; }
        @Override public int dimensoes() { return dim; }
        @Override
        public float[] embed(String texto) {
            float[] v = new float[dim];
            if (texto == null) return v;
            int h = texto.toLowerCase().hashCode();
            for (int i = 0; i < dim; i++) v[i] = ((h >> (i % 24)) & 0xff) / 255f;
            float norm = 0;
            for (float f : v) norm += f * f;
            norm = (float) Math.sqrt(norm);
            if (norm > 0) for (int i = 0; i < dim; i++) v[i] /= norm;
            return v;
        }
    }

    static class InMemoryVectorStore implements VectorStore {
        final Map<UUID, VectorRecord> records = new ConcurrentHashMap<>();
        @Override public void upsert(VectorRecord r) { records.put(r.id(), r); }
        @Override public void upsertAll(List<VectorRecord> list) { list.forEach(this::upsert); }
        @Override public void deleteById(UUID id) { records.remove(id); }
        @Override public void deleteByDocumentoId(UUID documentoId) {
            records.values().removeIf(r -> documentoId.equals(r.documentoId()));
        }
        @Override public void deleteByArtigoId(UUID artigoId) {
            records.values().removeIf(r -> artigoId.equals(r.artigoId()));
        }
        @Override
        public List<VectorSearchHit> search(VectorSearchRequest req) {
            List<VectorSearchHit> hits = new ArrayList<>();
            for (VectorRecord r : records.values()) {
                if (req.diplomaId() != null && !req.diplomaId().equals(r.diplomaId())) continue;
                if (req.modeloEmbedding() != null && !req.modeloEmbedding().equals(r.modeloEmbedding())) continue;
                double dist = cosineDistance(req.query(), r.embedding());
                hits.add(new VectorSearchHit(r.id(), r.artigoId(), r.diplomaId(), r.documentoId(),
                        r.texto(), dist, r.metadadosJson()));
            }
            hits.sort((a, b) -> Double.compare(a.distance(), b.distance()));
            int lim = req.limite() > 0 ? req.limite() : 10;
            return hits.size() > lim ? hits.subList(0, lim) : hits;
        }
        private static double cosineDistance(float[] a, float[] b) {
            if (a == null || b == null || a.length == 0) return 1.0;
            int n = Math.min(a.length, b.length);
            double dot = 0, na = 0, nb = 0;
            for (int i = 0; i < n; i++) {
                dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i];
            }
            if (na == 0 || nb == 0) return 1.0;
            return 1.0 - (dot / (Math.sqrt(na) * Math.sqrt(nb)));
        }
    }
}
