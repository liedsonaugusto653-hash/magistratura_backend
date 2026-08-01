package ao.magistratura.config;

import ao.magistratura.knowledge.vector.NoOpVectorStore;
import ao.magistratura.knowledge.vector.VectorStore;
import ao.magistratura.pipeline.index.KnowledgeIndexer;
import ao.magistratura.pipeline.index.NoOpKnowledgeIndexer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Em profile prod/production: impede arranque com stubs perigosos
 * (vector store NoOp, indexer NoOp, email só em log).
 */
@Component
public class ProductionStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupValidator.class);

    private final Environment environment;
    private final ObjectProvider<VectorStore> vectorStore;
    private final ObjectProvider<KnowledgeIndexer> knowledgeIndexer;
    private final String vectorStoreProp;
    private final String indexerProp;
    private final String emailMode;

    public ProductionStartupValidator(
            Environment environment,
            ObjectProvider<VectorStore> vectorStore,
            ObjectProvider<KnowledgeIndexer> knowledgeIndexer,
            @Value("${app.knowledge.vector-store:noop}") String vectorStoreProp,
            @Value("${app.knowledge.indexer:noop}") String indexerProp,
            @Value("${app.email.mode:logging}") String emailMode
    ) {
        this.environment = environment;
        this.vectorStore = vectorStore;
        this.knowledgeIndexer = knowledgeIndexer;
        this.vectorStoreProp = vectorStoreProp;
        this.indexerProp = indexerProp;
        this.emailMode = emailMode;
    }

    @PostConstruct
    public void validate() {
        VectorStore vs = vectorStore.getIfAvailable();
        KnowledgeIndexer idx = knowledgeIndexer.getIfAvailable();

        if (!isProd()) {
            log.info("Startup checks (dev): vector-store={}, indexer={}, email.mode={}",
                    vectorStoreProp, indexerProp, emailMode);
            if (vs instanceof NoOpVectorStore || "noop".equalsIgnoreCase(vectorStoreProp)) {
                log.warn("VectorStore é NoOp — RAG semântico desactivado. Aceitável só em desenvolvimento.");
            }
            return;
        }

        if ("noop".equalsIgnoreCase(vectorStoreProp) || vs == null || vs instanceof NoOpVectorStore) {
            throw new IllegalStateException(
                    "FATAL (prod): app.knowledge.vector-store=noop / NoOpVectorStore. "
                            + "Defina KNOWLEDGE_VECTOR_STORE=pgvector e use PostgreSQL com extensão vector.");
        }

        if ("noop".equalsIgnoreCase(indexerProp) || idx == null || idx instanceof NoOpKnowledgeIndexer) {
            throw new IllegalStateException(
                    "FATAL (prod): KnowledgeIndexer ausente ou NoOp. "
                            + "Defina KNOWLEDGE_INDEXER=knowledge e garanta a implementação real no classpath.");
        }

        if (!"smtp".equalsIgnoreCase(emailMode)) {
            throw new IllegalStateException(
                    "FATAL (prod): app.email.mode deve ser 'smtp'. "
                            + "Configure spring.mail.* / MAIL_* para recuperação de password real.");
        }

        log.info("Startup prod OK: vector-store={}, indexer={}, email.mode=smtp",
                vectorStoreProp, indexerProp);
    }

    private boolean isProd() {
        for (String p : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return false;
    }
}
