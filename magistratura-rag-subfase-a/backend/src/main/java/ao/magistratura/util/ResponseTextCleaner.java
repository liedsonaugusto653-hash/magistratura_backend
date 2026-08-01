package ao.magistratura.util;

/**
 * Pós-processamento leve de respostas do Tutor (IA local).
 * Remove ecos de parágrafos quase idênticos sem alterar citações [n].
 */
public final class ResponseTextCleaner {

    private ResponseTextCleaner() {
    }

    /**
     * Remove parágrafos consecutivos com sobreposição elevada de palavras.
     * Não altera marcadores de citação nem números de artigos.
     */
    public static String removerParagrafosDuplicados(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto == null ? "" : texto;
        }
        String[] partes = texto.split("\\n\\s*\\n");
        if (partes.length < 2) {
            return texto.trim();
        }
        StringBuilder out = new StringBuilder();
        String anterior = null;
        for (String p : partes) {
            String atual = p.trim();
            if (atual.isEmpty()) {
                continue;
            }
            if (anterior != null && similaridadePalavras(anterior, atual) >= 0.82) {
                continue;
            }
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append(atual);
            anterior = atual;
        }
        return out.toString();
    }

    private static double similaridadePalavras(String a, String b) {
        String[] wa = a.toLowerCase().split("\\s+");
        String[] wb = b.toLowerCase().split("\\s+");
        if (wa.length == 0 || wb.length == 0) {
            return 0;
        }
        java.util.Set<String> sa = new java.util.HashSet<>();
        for (String w : wa) {
            if (w.length() > 2) {
                sa.add(w);
            }
        }
        if (sa.isEmpty()) {
            return 0;
        }
        int inter = 0;
        java.util.Set<String> sb = new java.util.HashSet<>();
        for (String w : wb) {
            if (w.length() > 2) {
                sb.add(w);
            }
        }
        for (String w : sb) {
            if (sa.contains(w)) {
                inter++;
            }
        }
        int uni = sa.size() + sb.size() - inter;
        return uni == 0 ? 0 : (double) inter / uni;
    }
}
