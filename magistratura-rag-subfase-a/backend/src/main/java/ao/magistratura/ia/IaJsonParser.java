package ao.magistratura.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extrai JSON de respostas de LLMs, tolerando markdown, texto à volta,
 * vírgulas finais e JSON truncado (recupera objectos completos do array).
 */
public final class IaJsonParser {

    private static final Logger log = LoggerFactory.getLogger(IaJsonParser.class);

    private IaJsonParser() {}

    public static JsonNode parse(ObjectMapper mapper, String respostaIA) {
        if (respostaIA == null || respostaIA.isBlank()) {
            throw new IllegalArgumentException("Resposta da IA vazia");
        }

        String limpo = limpar(respostaIA);
        Exception ultimo = null;

        for (String candidato : new String[]{
                limpo,
                repararVirgulasFinais(limpo),
                envolverArraySeNecessario(limpo),
                recuperarJsonTruncado(limpo, "questoes"),
                recuperarJsonTruncado(limpo, "flashcards")
        }) {
            if (candidato == null || candidato.isBlank()) continue;
            try {
                JsonNode node = mapper.readTree(candidato);
                if (node != null && !node.isNull()) {
                    return node;
                }
            } catch (Exception e) {
                ultimo = e;
            }
        }

        String amostra = respostaIA.length() > 500 ? respostaIA.substring(0, 500) + "…" : respostaIA;
        log.warn("Falha a parsear JSON da IA (len={}). Amostra: {}",
                respostaIA.length(), amostra.replaceAll("\\s+", " "));
        throw new IllegalArgumentException(
                "Nenhum JSON válido na resposta da IA"
                        + (ultimo != null ? ": " + ultimo.getMessage() : "")
                        + ". A resposta pode ter sido cortada — tenta gerar menos itens (1–3).");
    }

    public static JsonNode garantirArray(JsonNode raiz, String chave) {
        if (raiz == null) {
            throw new IllegalArgumentException("JSON nulo");
        }
        if (raiz.isArray()) {
            ObjectMapper m = new ObjectMapper();
            ObjectNode wrap = m.createObjectNode();
            wrap.set(chave, raiz);
            return wrap;
        }
        if (raiz.has(chave) && raiz.get(chave).isArray()) {
            return raiz;
        }
        for (String alt : new String[]{"questions", "items", "data", "resultado", "resultados", "cards"}) {
            if (raiz.has(alt) && raiz.get(alt).isArray()) {
                ObjectMapper m = new ObjectMapper();
                ObjectNode wrap = m.createObjectNode();
                wrap.set(chave, raiz.get(alt));
                return wrap;
            }
        }
        // Único objecto na raiz → array de 1
        if (raiz.isObject() && (raiz.has("enunciado") || raiz.has("pergunta") || raiz.has("frente"))) {
            ObjectMapper m = new ObjectMapper();
            ObjectNode wrap = m.createObjectNode();
            ArrayNode arr = wrap.putArray(chave);
            arr.add(raiz);
            return wrap;
        }
        throw new IllegalArgumentException("JSON sem array «" + chave + "»");
    }

    private static String limpar(String texto) {
        String t = texto.trim();
        if (t.contains("```")) {
            int a = t.indexOf("```");
            int b = t.indexOf('\n', a + 3);
            int c = t.indexOf("```", a + 3);
            if (c > a) {
                String bloco = (b > a && b < c) ? t.substring(b + 1, c) : t.substring(a + 3, c);
                t = bloco.trim();
                if (t.toLowerCase().startsWith("json")) {
                    t = t.substring(4).trim();
                }
            }
        }
        int obj = t.indexOf('{');
        int arr = t.indexOf('[');
        if (obj == -1 && arr == -1) {
            return t;
        }
        int inicio;
        char abre, fecha;
        if (arr >= 0 && (obj < 0 || arr < obj)) {
            inicio = arr;
            abre = '[';
            fecha = ']';
        } else {
            inicio = obj;
            abre = '{';
            fecha = '}';
        }
        int profundidade = 0;
        boolean emString = false;
        boolean escape = false;
        for (int i = inicio; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (emString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    emString = false;
                }
                continue;
            }
            if (ch == '"') {
                emString = true;
                continue;
            }
            if (ch == abre) {
                profundidade++;
            } else if (ch == fecha) {
                profundidade--;
                if (profundidade == 0) {
                    return t.substring(inicio, i + 1);
                }
            }
        }
        // Truncado: devolve do início até ao fim (recuperação trata disto)
        return t.substring(inicio);
    }

    private static String repararVirgulasFinais(String json) {
        return json.replaceAll(",\\s*([}\\]])", "$1");
    }

    private static String envolverArraySeNecessario(String json) {
        String t = json.trim();
        if (t.startsWith("[")) {
            return "{\"questoes\":" + t + "}";
        }
        return null;
    }

    /**
     * Quando o modelo corta a meio do array, extrai objectos {...} completos
     * e reconstrói {"chave":[ {...}, {...} ]}.
     */
    static String recuperarJsonTruncado(String texto, String chave) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto;
        // Preferir conteúdo após "chave":
        int keyIdx = indexOfIgnoreCase(t, "\"" + chave + "\"");
        if (keyIdx < 0) {
            keyIdx = indexOfIgnoreCase(t, "\"questions\"");
            if (keyIdx < 0) {
                keyIdx = indexOfIgnoreCase(t, "\"flashcards\"");
            }
            if (keyIdx < 0) {
                keyIdx = indexOfIgnoreCase(t, "\"cards\"");
            }
        }
        int searchFrom = keyIdx >= 0 ? keyIdx : 0;
        int arrStart = t.indexOf('[', searchFrom);
        if (arrStart < 0) {
            arrStart = t.indexOf('[');
        }
        if (arrStart < 0) {
            // Tentar objectos soltos
            return recuperarObjectosSoltos(t, chave);
        }

        StringBuilder items = new StringBuilder();
        int i = arrStart + 1;
        int count = 0;
        while (i < t.length()) {
            // skip whitespace and commas
            while (i < t.length() && (Character.isWhitespace(t.charAt(i)) || t.charAt(i) == ',')) {
                i++;
            }
            if (i >= t.length() || t.charAt(i) == ']') {
                break;
            }
            if (t.charAt(i) != '{') {
                // lixo ou truncado a meio de um valor
                break;
            }
            int end = fimObjectoBalanceado(t, i);
            if (end < 0) {
                // objecto incompleto — para
                break;
            }
            String obj = t.substring(i, end + 1);
            if (objectoPareceUtil(obj, chave)) {
                if (count > 0) {
                    items.append(',');
                }
                items.append(obj);
                count++;
            }
            i = end + 1;
        }
        if (count == 0) {
            return recuperarObjectosSoltos(t, chave);
        }
        return "{\"" + chave + "\":[" + items + "]}";
    }

    private static String recuperarObjectosSoltos(String t, String chave) {
        StringBuilder items = new StringBuilder();
        int count = 0;
        int i = 0;
        while (i < t.length()) {
            int start = t.indexOf('{', i);
            if (start < 0) {
                break;
            }
            int end = fimObjectoBalanceado(t, start);
            if (end < 0) {
                break;
            }
            String obj = t.substring(start, end + 1);
            if (objectoPareceUtil(obj, chave)) {
                if (count > 0) {
                    items.append(',');
                }
                items.append(obj);
                count++;
            }
            i = end + 1;
        }
        if (count == 0) {
            return null;
        }
        return "{\"" + chave + "\":[" + items + "]}";
    }

    private static boolean objectoPareceUtil(String obj, String chave) {
        String lower = obj.toLowerCase();
        if ("flashcards".equals(chave) || "cards".equals(chave)) {
            return lower.contains("\"pergunta\"") || lower.contains("\"frente\"")
                    || lower.contains("\"resposta\"") || lower.contains("\"verso\"");
        }
        // questoes
        return lower.contains("\"enunciado\"") || lower.contains("\"opcaoa\"")
                || lower.contains("\"respostacorreta\"");
    }

    /** Índice do '}' que fecha o objecto que começa em start ('{'). -1 se truncado. */
    private static int fimObjectoBalanceado(String t, int start) {
        if (start < 0 || start >= t.length() || t.charAt(start) != '{') {
            return -1;
        }
        int profundidade = 0;
        boolean emString = false;
        boolean escape = false;
        for (int i = start; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (emString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    emString = false;
                }
                continue;
            }
            if (ch == '"') {
                emString = true;
                continue;
            }
            if (ch == '{') {
                profundidade++;
            } else if (ch == '}') {
                profundidade--;
                if (profundidade == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int indexOfIgnoreCase(String hay, String needle) {
        return hay.toLowerCase().indexOf(needle.toLowerCase());
    }
}
