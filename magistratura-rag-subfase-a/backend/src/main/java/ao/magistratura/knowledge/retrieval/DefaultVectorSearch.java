package ao.magistratura.knowledge.retrieval;

import ao.magistratura.entity.Artigo;
import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.embedding.EmbeddingProvider;
import ao.magistratura.knowledge.vector.VectorSearchHit;
import ao.magistratura.knowledge.vector.VectorSearchRequest;
import ao.magistratura.knowledge.vector.VectorStore;
import ao.magistratura.repository.ArtigoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultVectorSearch implements VectorSearch {

    private static final Logger log = LoggerFactory.getLogger(DefaultVectorSearch.class);

    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final ArtigoRepository artigoRepository;

    @Override
    public List<KnowledgePassage> search(KnowledgeQuery query) {
        if (query.texto() == null || query.texto().isBlank()) {
            return List.of();
        }
        try {
            float[] q = embeddingProvider.embed("search_query: " + query.texto());
            int fetch = Math.max(query.limite() * 3, 15);
            List<VectorSearchHit> hits = vectorStore.search(new VectorSearchRequest(
                    q,
                    query.diplomaId(),
                    query.kind() != null ? query.kind() : KnowledgeContentKind.LEGISLACAO,
                    embeddingProvider.modelo(),
                    fetch));

            List<KnowledgePassage> out = new ArrayList<>();
            for (VectorSearchHit h : hits) {
                double score = 1.0 - Math.min(1.0, Math.max(0.0, h.distance()));
                Artigo art = h.artigoId() != null
                        ? artigoRepository.findById(h.artigoId()).orElse(null)
                        : null;
                out.add(new KnowledgePassage(
                        h.id(),
                        h.artigoId(),
                        h.diplomaId(),
                        h.documentoId(),
                        query.kind() != null ? query.kind() : KnowledgeContentKind.LEGISLACAO,
                        art != null && art.getDiploma() != null ? art.getDiploma().getTitulo() : null,
                        art != null && art.getDiploma() != null ? art.getDiploma().getNumero() : null,
                        art != null ? art.getNumero() : null,
                        art != null ? art.getTitulo() : null,
                        art != null ? art.getCapitulo() : null,
                        art != null ? art.getSeccao() : null,
                        h.texto(),
                        "VECTOR",
                        score));
            }
            return out;
        } catch (Exception e) {
            log.warn("Vector search indisponível ({}). Continua só lexical.", e.getMessage());
            return List.of();
        }
    }
}
