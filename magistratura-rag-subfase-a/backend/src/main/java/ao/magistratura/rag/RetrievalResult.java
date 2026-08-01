package ao.magistratura.rag;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;

import java.util.List;

/**
 * Resultado da recuperação jurídica (Subfase A — lexical / determinística).
 */
public record RetrievalResult(
        Diploma diploma,
        List<Artigo> artigos,
        List<RetrievedPassage> passagens,
        String estrategia,
        boolean vazio
) {

    /**
     * Cria um resultado sem contexto jurídico encontrado.
     */
    public static RetrievalResult semContexto() {
        return new RetrievalResult(
                null,
                List.of(),
                List.of(),
                "NENHUMA",
                true
        );
    }

    /**
     * Cria um resultado de recuperação.
     */
    public static RetrievalResult de(
            Diploma diploma,
            List<Artigo> artigos,
            List<RetrievedPassage> passagens,
            String estrategia
    ) {
        boolean empty = (artigos == null || artigos.isEmpty()) && diploma == null;

        return new RetrievalResult(
                diploma,
                artigos != null ? artigos : List.of(),
                passagens != null ? passagens : List.of(),
                estrategia,
                empty
        );
    }
}