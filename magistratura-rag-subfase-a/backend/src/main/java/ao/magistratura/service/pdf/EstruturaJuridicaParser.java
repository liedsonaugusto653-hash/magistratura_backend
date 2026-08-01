package ao.magistratura.service.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestra {@link CapituloDetector}, {@link SeccaoDetector} e
 * {@link RegexArtigoDetector} sobre o texto extraído de um PDF, e produz
 * uma lista ordenada de {@link ArtigoExtraido}, cada um já associado ao
 * capítulo/secção vigente nesse ponto do documento e à página onde começa
 * e acaba.
 * <p>
 * Esta classe não faz I/O nem persistência — recebe texto já extraído
 * (por {@link PdfExtractorService}) e devolve dados em memória. Isto
 * mantém o pipeline totalmente testável sem precisar de ficheiros PDF
 * reais nos testes unitários.
 * <p>
 * <b>Frente A — índice vs corpo:</b> entradas do índice/sumário (linhas
 * "Artigo N …… página") não são materializadas como artigos. Quando o
 * mesmo número aparece no índice e no corpo normativo, prevalece o corpo.
 */
@Component
@RequiredArgsConstructor
public class EstruturaJuridicaParser {

    /** Contagem solta usada apenas como heurística de validação (ver DocumentoService). */
    private static final Pattern OCORRENCIA_SOLTA_ARTIGO =
            Pattern.compile("(?i)\\bartigo\\s+\\d+");

    /**
     * Cabeçalhos típicos de índice/sumário (não são "CAPÍTULO" no sentido
     * normativo, mas delimitam a zona a ignorar para artigos).
     */
    private static final Pattern CABECALHO_INDICE = Pattern.compile(
            "(?im)^\\s*(índice(?:\\s+analítico)?|indice(?:\\s+analitico)?|sumário|sumario|"
                    + "tabela\\s+de\\s+conte[uú]dos)\\s*[:.\\-–—]?\\s*$");

    /** Linha clássica de TOC: "Artigo 1.º .......... 12". */
    private static final Pattern LINHA_INDICE_ARTIGO = Pattern.compile(
            "(?is)^\\s*art(?:igo|\\.)?\\s*\\d{1,4}.*?\\.{3,}\\s*\\d{1,4}\\s*$");

    private final CapituloDetector capituloDetector;
    private final SeccaoDetector seccaoDetector;
    private final RegexArtigoDetector artigoDetector;
    private final PretextoJuridicoDetector pretextoDetector;

    public record Resultado(List<ArtigoExtraido> artigos, int ocorrenciasSoltasArtigo) {
    }

    public Resultado processar(List<PaginaTexto> paginas) {
        // Concatena todas as páginas num único texto, guardando o offset onde
        // cada página começa, para depois mapear posição -> número de página.
        StringBuilder textoCompleto = new StringBuilder();
        List<int[]> offsetsPagina = new ArrayList<>(); // [offsetInicio, numeroPagina]

        for (PaginaTexto pagina : paginas) {
            offsetsPagina.add(new int[]{textoCompleto.length(), pagina.numeroPagina()});
            textoCompleto.append(pagina.texto()).append('\n');
        }

        String texto = textoCompleto.toString();

        // Delimita capa / índice / preâmbulo / exposição de motivos vs. corpo de estudo
        int inicioCorpo = pretextoDetector.offsetInicioCorpo(texto);

        List<Marco> marcos = new ArrayList<>();
        marcos.addAll(capituloDetector.detetar(texto));
        marcos.addAll(seccaoDetector.detetar(texto));
        List<Marco> marcosArtigo = artigoDetector.detetar(texto);
        marcos.addAll(marcosArtigo);
        marcos.sort((a, b) -> Integer.compare(a.posicao(), b.posicao()));

        // Cabeçalhos de índice + restantes zonas pré-textuais
        List<Integer> iniciosIndice = localizarCabecalhosIndice(texto);
        iniciosIndice.addAll(pretextoDetector.offsetsCabecalhosPretexto(texto));
        iniciosIndice.sort(Integer::compareTo);

        List<ArtigoExtraido> artigosBrutos = new ArrayList<>();
        String capituloAtual = null;
        String seccaoAtual = null;
        int ordem = 0;
        boolean dentroDeZonaIndice = false;

        for (int i = 0; i < marcos.size(); i++) {
            Marco marco = marcos.get(i);

            // Entrámos num cabeçalho de índice/preâmbulo entre o marco anterior e este?
            if (acabouDeEntrarEmIndice(iniciosIndice, i == 0 ? 0 : marcos.get(i - 1).posicao(), marco.posicao())) {
                dentroDeZonaIndice = true;
            }

            switch (marco.tipo()) {
                case CAPITULO -> {
                    capituloAtual = formatarRotulo("Capítulo", marco.rotulo(), marco.titulo());
                    seccaoAtual = null;
                    if (pretextoDetector.linhaOuRotuloPretexto(capituloAtual)
                            || rotuloPareceIndice(capituloAtual)) {
                        dentroDeZonaIndice = true;
                    } else if (marco.posicao() >= inicioCorpo) {
                        dentroDeZonaIndice = false;
                    }
                }
                case SECCAO -> {
                    seccaoAtual = formatarRotulo("Secção", marco.rotulo(), marco.titulo());
                    if (pretextoDetector.linhaOuRotuloPretexto(seccaoAtual)
                            || rotuloPareceIndice(seccaoAtual)) {
                        dentroDeZonaIndice = true;
                    }
                }
                case ARTIGO -> {
                    int inicioTexto = marco.posicao();
                    int fimTexto = proximoMarco(marcos, i + 1, texto.length());
                    String conteudo = texto.substring(inicioTexto, fimTexto).trim();

                    // Antes do corpo normativo estimado → pré-texto (índice, preâmbulo, etc.)
                    if (inicioTexto < inicioCorpo) {
                        // Excepção: amostra já é claramente corpo (falso positivo do offset)
                        if (isBlocoIndice(conteudo) || conteudo.length() < 80
                                || PretextoJuridicoDetector.qualidadeAmostraCorpo(conteudo) < 50) {
                            continue;
                        }
                        // Corpo detectado mais cedo do que a heurística — avança inicioCorpo
                        inicioCorpo = inicioTexto;
                    }

                    // Zona de índice/preâmbulo explícita
                    if (dentroDeZonaIndice && !isBlocoIndice(conteudo) && conteudo.length() > 80
                            && PretextoJuridicoDetector.qualidadeAmostraCorpo(conteudo) >= 50) {
                        dentroDeZonaIndice = false;
                    }

                    if (dentroDeZonaIndice
                            || rotuloPareceIndice(capituloAtual)
                            || rotuloPareceIndice(seccaoAtual)
                            || pretextoDetector.linhaOuRotuloPretexto(capituloAtual)
                            || pretextoDetector.linhaOuRotuloPretexto(seccaoAtual)
                            || isBlocoIndice(conteudo)
                            || pareceEntradaDeIndiceCurta(conteudo)) {
                        continue;
                    }

                    int paginaInicio = paginaDaPosicao(offsetsPagina, inicioTexto);
                    int paginaFim = paginaDaPosicao(offsetsPagina, Math.max(inicioTexto, fimTexto - 1));

                    ordem++;
                    artigosBrutos.add(new ArtigoExtraido(
                            marco.rotulo(),
                            marco.titulo(),
                            conteudo,
                            ordem,
                            capituloAtual,
                            seccaoAtual,
                            paginaInicio,
                            paginaFim
                    ));
                }
            }
        }

        List<ArtigoExtraido> artigos = deduplicarPreferindoCorpo(artigosBrutos);
        // Ordem legal: número do artigo crescente (1, 2, 3…), não ordem de aparição no PDF/OCR
        artigos = ordenarPorNumeroCrescente(artigos);
        List<ArtigoExtraido> finais = new ArrayList<>(artigos.size());
        for (int i = 0; i < artigos.size(); i++) {
            ArtigoExtraido a = artigos.get(i);
            finais.add(new ArtigoExtraido(
                    a.numero(), a.titulo(), a.texto(), i + 1,
                    a.capitulo(), a.seccao(), a.paginaInicio(), a.paginaFim()));
        }

        int ocorrenciasSoltas = contarOcorrenciasSoltas(texto);
        return new Resultado(finais, ocorrenciasSoltas);
    }

    /**
     * Ordena por número de artigo (inteiro extraído de {@code numero}), depois página, depois ordem bruta.
     * Garante lista 1 → 2 → 3 … em vez de ordem caótica de OCR/índice.
     */
    public static List<ArtigoExtraido> ordenarPorNumeroCrescente(List<ArtigoExtraido> lista) {
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        List<ArtigoExtraido> copia = new ArrayList<>(lista);
        copia.sort(Comparator
                .comparingInt((ArtigoExtraido a) -> numeroOrdenavel(a.numero()))
                .thenComparingInt(a -> a.paginaInicio() != null ? a.paginaInicio() : 0)
                .thenComparingInt(a -> a.ordem() != null ? a.ordem() : 0));
        return copia;
    }

    /**
     * Extrai o primeiro inteiro de "1", "1.º", "Art. 12", "XII" (só dígitos arábicos).
     * Sem dígitos → {@link Integer#MAX_VALUE} (vai para o fim).
     */
    static int numeroOrdenavel(String numero) {
        if (numero == null || numero.isBlank()) {
            return Integer.MAX_VALUE;
        }
        Matcher m = Pattern.compile("(\\d{1,6})").matcher(numero.trim());
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Se o mesmo número sobreviveu por falha de heurística (índice + corpo),
     * fica apenas o bloco com maior qualidade (corpo normativo).
     */
    public static List<ArtigoExtraido> deduplicarPreferindoCorpo(List<ArtigoExtraido> lista) {
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        Map<String, ArtigoExtraido> melhor = new LinkedHashMap<>();
        for (ArtigoExtraido a : lista) {
            if (a == null || a.numero() == null) {
                continue;
            }
            String chave = a.numero().trim();
            ArtigoExtraido atual = melhor.get(chave);
            if (atual == null || scoreQualidade(a) > scoreQualidade(atual)) {
                melhor.put(chave, a);
            }
        }
        List<ArtigoExtraido> out = new ArrayList<>(melhor.values());
        out.sort(Comparator
                .comparingInt((ArtigoExtraido a) -> numeroOrdenavel(a.numero()))
                .thenComparingInt(a -> a.paginaInicio() != null ? a.paginaInicio() : 0)
                .thenComparingInt(a -> a.ordem() != null ? a.ordem() : Integer.MAX_VALUE));
        return out;
    }

    /**
     * Maior = mais provavelmente corpo normativo (não linha de índice).
     */
    static int scoreQualidade(ArtigoExtraido a) {
        if (a == null) {
            return Integer.MIN_VALUE;
        }
        String t = a.texto() != null ? a.texto() : "";
        int score = Math.min(t.length(), 5000);
        if (isBlocoIndice(t)) {
            score -= 10_000;
        }
        if (rotuloPareceIndice(a.capitulo()) || rotuloPareceIndice(a.seccao())) {
            score -= 5_000;
        }
        return score;
    }

    /**
     * Conteúdo típico de índice: pontos líderes, só número de página, texto mínimo.
     */
    public static boolean isBlocoIndice(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            return false;
        }
        String trim = conteudo.trim();
        if (LINHA_INDICE_ARTIGO.matcher(trim).matches()) {
            return true;
        }
        String primeira = trim.lines().findFirst().orElse(trim).trim();
        if (LINHA_INDICE_ARTIGO.matcher(primeira).matches()) {
            return true;
        }
        long dots = trim.chars().filter(ch -> ch == '.').count();
        if (trim.length() > 10 && dots * 2 >= trim.length()) {
            return true;
        }
        String lower = trim.toLowerCase(Locale.ROOT);
        if (trim.length() < 100
                && trim.matches("(?is).*\\d{1,4}\\s*$")
                && (dots >= 3 || trim.contains("\t"))
                && !lower.matches("(?s).*(rep[uú]blica|direito|dever|princ[ií]pio|soberania|liberdade|estado).*")) {
            return true;
        }
        // TOC OCR sem pontos líderes: "Artigo 12 Direitos fundamentais  18"
        if (pareceEntradaDeIndiceCurta(trim)) {
            return true;
        }
        return false;
    }

    /**
     * Entrada de índice curta (comum em OCR de sumários sem ".....").
     * Ex.: "Artigo 5.º  Princípios fundamentais    12"
     */
    public static boolean pareceEntradaDeIndiceCurta(String conteudo) {
        if (conteudo == null) {
            return false;
        }
        String trim = conteudo.trim();
        if (trim.length() > 160) {
            return false;
        }
        // Uma ou duas linhas, cabeçalho Art. N, pouco texto, muitas vezes termina em dígitos (página)
        long linhas = trim.lines().count();
        if (linhas > 3) {
            return false;
        }
        String lower = trim.toLowerCase(Locale.ROOT);
        boolean cabArt = lower.matches("(?s)^\\s*art(?:igo|\\.)\\s*\\d{1,4}.*");
        if (!cabArt) {
            return false;
        }
        // Pouco léxico normativo no corpo da entrada
        boolean temNorma = lower.matches("(?s).*(rep[uú]blica|soberania|liberdade|obrigat[oó]rio|"
                + "contrato|pena de|prescreve|é nulo|compete ao|nos termos).*");
        if (temNorma && trim.length() > 90) {
            return false;
        }
        // Termina em número de página isolado ou tabulação
        if (trim.matches("(?is).*(\\s{2,}|\\t)\\d{1,4}\\s*$") || trim.matches("(?is).*\\d{1,4}\\s*$")) {
            // Sem pontuação de frase normativa
            if (!trim.contains(";") && !lower.contains(" nos ") && trim.length() < 140) {
                return true;
            }
        }
        // Muito curto: só rótulo + título telegráfico
        return trim.length() < 55 && !temNorma;
    }

    static boolean rotuloPareceIndice(String rotulo) {
        if (rotulo == null || rotulo.isBlank()) {
            return false;
        }
        String t = rotulo.toLowerCase(Locale.ROOT);
        return t.contains("índice") || t.contains("indice")
                || t.contains("sumário") || t.contains("sumario")
                || t.contains("tabela de conteúdo") || t.contains("tabela de conteudo");
    }

    private static List<Integer> localizarCabecalhosIndice(String texto) {
        List<Integer> pos = new ArrayList<>();
        Matcher m = CABECALHO_INDICE.matcher(texto);
        while (m.find()) {
            pos.add(m.start());
        }
        return pos;
    }

    private static boolean acabouDeEntrarEmIndice(List<Integer> inicios, int desde, int ate) {
        for (int p : inicios) {
            if (p >= desde && p < ate) {
                return true;
            }
            // cabeçalho exactamente neste marco (raro)
            if (p == ate) {
                return true;
            }
        }
        return false;
    }

    private int proximoMarco(List<Marco> marcos, int aPartirDe, int fimPadrao) {
        if (aPartirDe < marcos.size()) {
            return marcos.get(aPartirDe).posicao();
        }
        return fimPadrao;
    }

    private int paginaDaPosicao(List<int[]> offsetsPagina, int posicao) {
        int pagina = offsetsPagina.isEmpty() ? 1 : offsetsPagina.get(0)[1];
        for (int[] offset : offsetsPagina) {
            if (offset[0] <= posicao) {
                pagina = offset[1];
            } else {
                break;
            }
        }
        return pagina;
    }

    private String formatarRotulo(String prefixo, String rotulo, String titulo) {
        return (titulo == null || titulo.isBlank())
                ? prefixo + " " + rotulo
                : prefixo + " " + rotulo + " — " + titulo;
    }

    /**
     * Contagem aproximada (e deliberadamente "ingénua") de menções a
     * "Artigo N" no texto bruto, usada só para detetar grandes desvios
     * face ao número de artigos efetivamente estruturados — não para
     * estruturar o documento. Tende a sobrestimar (apanha também citações
     * a outros artigos dentro do próprio texto), por isso serve apenas
     * como sinal de alarme, não como valor exato esperado.
     */
    private int contarOcorrenciasSoltas(String texto) {
        Matcher m = OCORRENCIA_SOLTA_ARTIGO.matcher(texto);
        int contagem = 0;
        while (m.find()) {
            contagem++;
        }
        return contagem;
    }
}
