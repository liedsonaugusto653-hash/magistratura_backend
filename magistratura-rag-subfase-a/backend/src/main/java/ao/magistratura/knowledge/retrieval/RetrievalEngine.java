package ao.magistratura.knowledge.retrieval;

import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;

public interface RetrievalEngine {
    KnowledgeResult search(KnowledgeQuery query);
}
