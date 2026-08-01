package ao.magistratura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Distingue material <b>pré-textual</b> (capa, índice, preâmbulo, exposição de
 * motivos, considerandos) do <b>corpo normativo</b> — os artigos que o estudante
 * deve estudar.
 * <p>
 * Heurísticas determinísticas (sem LLM), calibradas para diplomas angolanos
 * e portugueses típicos (Constituição, códigos, Diário da República).
 */
@Component
public class PretextoJuridicoDetector {

    /**
     * Cabeçalhos de zonas que o estudante não quer como "artigos" de estudo.
     */
    private static final Pattern CABECALHO_PRETEXTO = Pattern.compile(
            "(?im)^\\s*(?:"
                    + "pre[aâ]mbulo"
                    + "|exposi[cç][aã]o\\s+de\\s+motivos"
                    + "|nota\\s+(?:preliminar|introdut[oó]ria|explicativa)"
                    + "|introdu[cç][aã]o"
                    + "|considerandos?"
                    + "|hist[oó]rico(?:\\s+legislativo)?"
                    + "|apresenta[cç][aã]o"
                    + "|aviso\\s+(?:ao\\s+)?leitor"
                    + "|nota\\s+de\\s+edi[cç][aã]o"
                    + "|ficha\\s+t[eé]cnica"
                    + "|[ií]ndice(?:\\s+anal[ií]tico|\\s+sistem[aá]tico|\\s+remissivo)?"
                    + "|indice(?:\\s+analitico|\\s+sistematico|\\s+remissivo)?"
                    + "|sum[aá]rio"
                    + "|tabela\\s+de\\s+conte[uú]dos"
                    + "|conte[uú]do"
                    + ")\\s*[:.\\-–—]?\\s*$");

    /**
     * Fórmulas que tipicamente <b>abrem o corpo</b> da lei (após preâmbulo).
     */
    private static final Pattern MARCADOR_INICIO_CORPO = Pattern.compile(
            "(?im)^\\s*(?:"
                    + "a\\s+assembleia\\s+nacional\\s+decreta"
                    + "|o\\s+presidente\\s+da\\s+rep[uú]blica\\s+decreta"
                    + "|no\\s+uso\\s+da\\s+faculdade"
                    + "|nos\\s+termos\\s+(?:dos?|das?)\\s+artigos?"
                    + "|t[ií]tulo\\s+(?:i|ii|iii|iv|v|vi|vii|viii|ix|x|[0-9]+)\\b"
                    + "|cap[ií]tulo\\s+(?:i|ii|iii|iv|v|vi|vii|viii|ix|x|[0-9]+|[a-z])\\b"
                    + "|parte\\s+(?:i|ii|iii|iv|v|[0-9]+)\\b"
                    + "|livro\\s+(?:i|ii|iii|iv|v|[0-9]+)\\b"
                    + ")\\b");

    /** Artigo 1.º / Artigo 1 — candidato a início do articulado. */
    private static final Pattern ARTIGO_UM = Pattern.compile(
            "(?im)^\\s*art(?:igo|\\.)\\s*1\\s*\\.?\\s*[º°o]?\\b");

    /**
     * Estima o offset (carácter) onde começa o corpo útil para estudo.
     * Texto antes deste ponto trata-se como pré-texto (não materializar artigos).
     *
     * @return offset inclusivo; 0 se não for possível delimitar
     */
    public int offsetInicioCorpo(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }

        int marcador = primeiraOcorrencia(MARCADOR_INICIO_CORPO, texto);
        int artigoUmCorpo = primeiroArtigoUmComCorpo(texto);

        // Preferir o mais cedo entre marcador estrutural e 1.º artigo "de verdade",
        // mas nunca um artigo-um que ainda seja linha de índice.
        if (marcador >= 0 && artigoUmCorpo >= 0) {
            // Se o Art. 1 de corpo está logo após o marcador, usar o Art. 1
            // (o estudante estuda a partir do articulado, não da fórmula "decreta").
            if (artigoUmCorpo >= marcador && artigoUmCorpo - marcador < 2_000) {
                return artigoUmCorpo;
            }
            // Marcador depois de um falso Art. 1 de índice → usar marcador
            if (marcador > artigoUmCorpo) {
                return Math.min(marcador, artigoUmCorpo);
            }
            return Math.min(marcador, artigoUmCorpo);
        }
        if (artigoUmCorpo >= 0) {
            return artigoUmCorpo;
        }
        if (marcador >= 0) {
            return marcador;
        }

        // Fallback: após o último cabeçalho de pré-texto na primeira metade do doc
        int ultimoPre = ultimoCabecalhoPretextoNaPrimeiraMetade(texto);
        if (ultimoPre >= 0) {
            return Math.min(texto.length(), ultimoPre + 1);
        }
        return 0;
    }

    /**
     * Indica se a posição {@code offset} ainda está dentro de zona de pré-texto
     * (antes do corpo ou dentro de um bloco explícito de índice/preâmbulo).
     */
    public boolean isZonaPretexto(String texto, int offset, int inicioCorpo) {
        if (offset < inicioCorpo) {
            return true;
        }
        // Após inicioCorpo ainda pode haver um índice intercalado (raro) —
        // verificado pelo parser via cabeçalhos.
        return false;
    }

    public List<Integer> offsetsCabecalhosPretexto(String texto) {
        List<Integer> pos = new ArrayList<>();
        if (texto == null) {
            return pos;
        }
        Matcher m = CABECALHO_PRETEXTO.matcher(texto);
        while (m.find()) {
            pos.add(m.start());
        }
        return pos;
    }

    public boolean linhaOuRotuloPretexto(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String t = s.toLowerCase(Locale.ROOT).trim();
        if (CABECALHO_PRETEXTO.matcher(s).find()) {
            return true;
        }
        return t.contains("preâmbulo") || t.contains("preambulo")
                || t.contains("exposição de motivos") || t.contains("exposicao de motivos")
                || t.contains("índice") || t.contains("indice")
                || t.contains("sumário") || t.contains("sumario")
                || t.contains("considerandos")
                || t.contains("nota preliminar") || t.contains("nota introdutória")
                || t.contains("nota introdutoria");
    }

    // ------------------------------------------------------------------

    private static int primeiraOcorrencia(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.start() : -1;
    }

    /**
     * Primeiro "Artigo 1" cujo bloco seguinte parece norma (não linha de TOC).
     */
    private int primeiroArtigoUmComCorpo(String texto) {
        Matcher m = ARTIGO_UM.matcher(texto);
        while (m.find()) {
            int start = m.start();
            int end = Math.min(texto.length(), start + 400);
            String amostra = texto.substring(start, end);
            if (!EstruturaJuridicaParser.isBlocoIndice(amostra) && qualidadeAmostraCorpo(amostra) >= 40) {
                return start;
            }
        }
        return -1;
    }

    /**
     * Pontuação simples da amostra após o cabeçalho do artigo.
     */
    static int qualidadeAmostraCorpo(String amostra) {
        if (amostra == null) {
            return 0;
        }
        String t = amostra.trim();
        int score = Math.min(t.length(), 300);
        long dots = t.chars().filter(ch -> ch == '.').count();
        if (t.length() > 15 && dots * 2 >= t.length()) {
            score -= 200; // líderes de TOC
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.matches("(?s).*(republica|república|direito|dever|principio|princípio|"
                + "soberania|liberdade|estado|cidadao|cidadão|pena|contrato|obrigacao|obrigação).*")) {
            score += 80;
        }
        // Só "Artigo 1.º .... 12"
        if (t.length() < 80 && t.matches("(?is).*\\d{1,4}\\s*$") && dots >= 3) {
            score -= 150;
        }
        return score;
    }

    private int ultimoCabecalhoPretextoNaPrimeiraMetade(String texto) {
        int metade = texto.length() / 2;
        int ultimo = -1;
        Matcher m = CABECALHO_PRETEXTO.matcher(texto);
        while (m.find()) {
            if (m.start() <= metade) {
                ultimo = m.start();
            }
        }
        return ultimo;
    }
}
