package ao.magistratura.knowledge.index;

import ao.magistratura.entity.Artigo;
import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.chunk.LegalChunker;
import ao.magistratura.knowledge.embedding.EmbeddingProvider;
import ao.magistratura.knowledge.vector.VectorRecord;
import ao.magistratura.knowledge.vector.VectorStore;
import ao.magistratura.repository.ArtigoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Indexa artigos no VectorStore (chunk + embed).
 * Usado pelo pipeline via {@link KnowledgeLayerIndexer}.
 */
@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private static boolean isAllZero(float[] vetor) {
        for (float v : vetor) {
            if (v != 0.0f) {
                return false;
            }
        }
        return true;
    }

    private final ArtigoRepository artigoRepository;
    private final LegalChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public int indexArtigo(UUID artigoId) {
        Artigo a = artigoRepository.findById(artigoId).orElse(null);
        if (a == null) {
            log.warn("indexArtigo: artigo {} não encontrado", artigoId);
            return 0;
        }
        vectorStore.deleteByArtigoId(artigoId);
        List<VectorRecord> batch = new ArrayList<>();
        for (LegalChunker.Chunk c : chunker.chunkArtigo(a)) {
            float[] emb;
            try {
                emb = embeddingProvider.embed("search_document: " + c.texto());
            } catch (Exception e) {
                log.warn("Embedding falhou para artigo {} chunk {}: {}", artigoId, c.indice(), e.getMessage());
                continue;
            }
            if (emb == null || emb.length == 0 || isAllZero(emb)) {
                log.warn("Embedding vazio/zero para artigo {} chunk {} — chunk não indexado no vector store",
                        artigoId, c.indice());
                continue;
            }
            String meta = "{}";
            try {
                meta = objectMapper.writeValueAsString(Map.of(
                        "artigoNumero", nullToEmpty(a.getNumero()),
                        "artigoTitulo", nullToEmpty(a.getTitulo()),
                        "capitulo", nullToEmpty(a.getCapitulo()),
                        "seccao", nullToEmpty(a.getSeccao()),
                        "chunkIndex", c.indice(),
                        "hash", c.hash()));
            } catch (Exception ignored) {
                // metadados opcionais
            }
            batch.add(new VectorRecord(
                    c.id(),
                    a.getId(),
                    a.getDiploma() != null ? a.getDiploma().getId() : null,
                    a.getDocumento() != null ? a.getDocumento().getId() : null,
                    KnowledgeContentKind.LEGISLACAO,
                    c.texto(),
                    emb,
                    embeddingProvider.modelo(),
                    meta));
        }
        vectorStore.upsertAll(batch);
        log.info("Indexados {} chunks do artigo {}", batch.size(), artigoId);
        return batch.size();
    }

    @Transactional
    public void removeByDocumento(UUID documentoId) {
        vectorStore.deleteByDocumentoId(documentoId);
    }

    @Transactional
    public void removeByArtigo(UUID artigoId) {
        vectorStore.deleteByArtigoId(artigoId);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}