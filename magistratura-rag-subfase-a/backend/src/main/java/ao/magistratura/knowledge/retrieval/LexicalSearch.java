package ao.magistratura.knowledge.retrieval;

import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;

import java.util.List;

public interface LexicalSearch {
    List<KnowledgePassage> search(KnowledgeQuery query);
}
