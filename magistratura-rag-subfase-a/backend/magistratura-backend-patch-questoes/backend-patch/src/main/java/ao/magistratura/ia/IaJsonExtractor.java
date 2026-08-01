package ao.magistratura.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Extracção robusta de JSON a partir de respostas de modelos locais (Ollama).
 * Modelos pequenos (ex.: llama3.2:3b) frequentemente devolvem markdown, texto
 * extra, chaves em inglês ou JSON truncado — esta classe tolera esses casos.
 */
public final class IaJsonExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IaJsonExtractor() {}

    /**
     * Isola e faz parse do JSON na resposta da IA.
     * Aceita: bloco cru, fences markdown ```json ... ```, texto antes/depois.
     */
    public static JsonNode parseObjectOrArray(String respostaIA) throws Exception {
        if (respostaIA == null || respostaIA.isBlank()) {
            throw new IllegalArgumentException("Resposta da IA vazia");
        }
        String texto = respostaIA.trim();

        // Remove fences markdown
        texto = texto.replaceAll("(?i)```json\\s*", "");
        texto = texto.replaceAll("(?i)```\\s*", "");
        texto = texto.trim();

        // Tenta parse directo
        try {
            return MAPPER.readTree(texto);
        } catch (Exception ignored) {
            // continua
        }

        // Primeiro objecto {...} ou array [...]
        int objIni = texto.indexOf('{');
        int arrIni = texto.indexOf('[');
        int inicio;
        char abre;
        char fecha;
        if (objIni >= 0 && (arrIni < 0 || objIni < arrIni)) {
            inicio = objIni;
            abre = '{';
            fecha = '}';
        } else if (arrIni >= 0) {
            inicio = arrIni;
            abre = '[';
            fecha = ']';
        } else {
            throw new IllegalArgumentException("Nenhum JSON encontrado na resposta da IA");
        }

        // Balanceamento de parênteses (respeita strings)
        int profundidade = 0;
        boolean emString = false;
        boolean escape = false;
        int fim = -1;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (emString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    emString = false;
                }
                continue;
            }
            if (c == '"') {
                emString = true;
                continue;
            }
            if (c == abre) {
                profundidade++;
            } else if (c == fecha) {
                profundidade--;
                if (profundidade == 0) {
                    fim = i;
                    break;
                }
            }
        }
        if (fim < 0) {
            // JSON possivelmente truncado: tenta reparar fechando chaves
            String parcial = texto.substring(inicio);
            String reparado = tentarRepararJsonTruncado(parcial, abre);
            return MAPPER.readTree(reparado);
        }
        return MAPPER.readTree(texto.substring(inicio, fim + 1));
    }

    private static String tentarRepararJsonTruncado(String parcial, char abre) {
        StringBuilder sb = new StringBuilder(parcial.trim());
        // Remove vírgula final pendente
        while (sb.length() > 0) {
            char last = sb.charAt(sb.length() - 1);
            if (last == ',' || Character.isWhitespace(last)) {
                sb.setLength(sb.length() - 1);
            } else {
                break;
            }
        }
        // Fecha strings abertas (heurística simples)
        int quotes = 0;
        boolean esc = false;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (esc) {
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            if (c == '"') {
                quotes++;
            }
        }
        if (quotes % 2 != 0) {
            sb.append('"');
        }
        int abertosObj = 0, abertosArr = 0;
        boolean inStr = false;
        esc = false;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (inStr) {
                if (esc) {
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
            } else if (c == '{') {
                abertosObj++;
            } else if (c == '}') {
                abertosObj--;
            } else if (c == '[') {
                abertosArr++;
            } else if (c == ']') {
                abertosArr--;
            }
        }
        while (abertosArr > 0) {
            sb.append(']');
            abertosArr--;
        }
        while (abertosObj > 0) {
            sb.append('}');
            abertosObj--;
        }
        return sb.toString();
    }

    /**
     * Obtém o array de questões a partir da raiz JSON, tolerando várias formas.
     */
    public static ArrayNode arrayQuestoes(JsonNode raiz) {
        if (raiz == null || raiz.isNull()) {
            return MAPPER.createArrayNode();
        }
        if (raiz.isArray()) {
            return (ArrayNode) raiz;
        }
        for (String key : new String[]{"questoes", "questions", "items", "data"}) {
            JsonNode n = raiz.path(key);
            if (n.isArray()) {
                return (ArrayNode) n;
            }
        }
        // objecto único com enunciado
        if (raiz.has("enunciado") || raiz.has("question") || raiz.has("pergunta")) {
            ArrayNode arr = MAPPER.createArrayNode();
            arr.add(raiz);
            return arr;
        }
        return MAPPER.createArrayNode();
    }

    public static String texto(JsonNode item, String... keys) {
        for (String k : keys) {
            JsonNode n = item.path(k);
            if (!n.isMissingNode() && !n.isNull()) {
                String v = n.asText("").trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return "";
    }

    /**
     * Normaliza resposta correta para A|B|C|D.
     */
    public static String normalizarRespostaCorreta(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim().toUpperCase();
        // "A", "A)", "A.", "OPCAO A", "ALTERNATIVA B", etc.
        if (s.matches("^[A-D].*")) {
            return s.substring(0, 1);
        }
        if (s.contains("OPCAO A") || s.contains("OPÇÃO A") || s.contains("ALTERNATIVA A")) {
            return "A";
        }
        if (s.contains("OPCAO B") || s.contains("OPÇÃO B") || s.contains("ALTERNATIVA B")) {
            return "B";
        }
        if (s.contains("OPCAO C") || s.contains("OPÇÃO C") || s.contains("ALTERNATIVA C")) {
            return "C";
        }
        if (s.contains("OPCAO D") || s.contains("OPÇÃO D") || s.contains("ALTERNATIVA D")) {
            return "D";
        }
        return s.length() == 1 ? s : "";
    }
}
