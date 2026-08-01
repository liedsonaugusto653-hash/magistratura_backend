package ao.magistratura.knowledge.retrieval;

import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;

import java.util.List;

public interface VectorSearch {
    List<KnowledgePassage> search(KnowledgeQuery query);
}
