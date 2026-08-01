package ao.magistratura.knowledge.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Política de contexto enviada ao LLM — evita documentos inteiros no prompt.
 * Objectivo: reduzir 70–90% dos tokens vs. enviar códigos completos.
 */
public final class StudyContextPolicy {

    private StudyContextPolicy() {}

    /** Chat / Tutor RAG */
    public static final int CHAT_TOP_K = 5;
    public static final int CHAT_MAX_PASSAGENS_NO_PROMPT = 4;

    /** Flashcards */
    public static final int FLASHCARD_TOP_K = 3;
    public static final int FLASHCARD_MAX_PASSAGENS = 3;

    /** Questões avulsas */
    public static final int QUESTAO_TOP_K = 4;
    public static final int QUESTAO_MAX_PASSAGENS = 4;

    /** Simulados */
    public static final int SIMULADO_TOP_K = 8;
    public static final int SIMULADO_MAX_PASSAGENS = 6;

    /** Truncagem por passagem (artigos longos → só o essencial) */
    public static final int MAX_CHARS_POR_PASSAGEM = 2200;

    public static List<KnowledgePassage> limitar(List<KnowledgePassage> passagens, int max) {
        if (passagens == null || passagens.isEmpty()) {
            return List.of();
        }
        if (passagens.size() <= max) {
            return passagens;
        }
        return passagens.subList(0, max);
    }

    /**
     * Cria novas passagens com texto truncado (não muta as originais).
     */
    public static List<KnowledgePassage> truncarTextos(List<KnowledgePassage> passagens, int maxChars) {
        if (passagens == null || passagens.isEmpty()) {
            return List.of();
        }
        List<KnowledgePassage> out = new ArrayList<>(passagens.size());
        for (KnowledgePassage p : passagens) {
            String texto = p.texto();
            if (texto != null && texto.length() > maxChars) {
                texto = texto.substring(0, maxChars) + "…";
            }
            out.add(p.comTextoTruncado(texto));
        }
        return out;
    }
}
