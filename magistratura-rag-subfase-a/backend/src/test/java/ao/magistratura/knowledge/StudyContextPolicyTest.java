package ao.magistratura.knowledge;

import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.StudyContextPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StudyContextPolicyTest {

    @Test
    void limitar_reduzLista() {
        List<KnowledgePassage> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new KnowledgePassage(
                    UUID.randomUUID(), UUID.randomUUID(), null, null,
                    KnowledgeContentKind.LEGISLACAO, "D", "1", String.valueOf(i),
                    null, null, null, "texto " + i, "T", 1.0));
        }
        var limited = StudyContextPolicy.limitar(list, 3);
        assertEquals(3, limited.size());
        assertEquals("0", limited.get(0).artigoNumero());
    }

    @Test
    void limitar_nullOuVazio() {
        assertTrue(StudyContextPolicy.limitar(null, 5).isEmpty());
        assertTrue(StudyContextPolicy.limitar(List.of(), 5).isEmpty());
    }
}
