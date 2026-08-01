package ao.magistratura.knowledge.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Única porta de entrada para conhecimento indexado / estruturado.
 * Consumidores de IA não devem aceder a repositories documentais.
 */
public interface KnowledgeService {

    KnowledgeResult search(KnowledgeQuery query);

    /** Artigo por id — devolve passagem única com texto completo. */
    Optional<KnowledgePassage> findArticle(UUID artigoId);

    /** Artigo por diploma + número (ex.: "5", "Art. 5"). */
    Optional<KnowledgePassage> findByReference(UUID diplomaId, String artigoNumero);

    void indexArtigo(UUID artigoId);

    void removeByDocumento(UUID documentoId);
}
