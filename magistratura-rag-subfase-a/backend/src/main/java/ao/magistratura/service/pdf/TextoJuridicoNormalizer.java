package ao.magistratura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Limpeza determinística do texto jurídico antes do parser estrutural.
 * Remove ruído típico de PDF/OCR sem alterar o conteúdo normativo.
 */
@Component
public class TextoJuridicoNormalizer {

    private static final Pattern ESPACOS_MULTIPLOS = Pattern.compile("[ \\t]{2,}");
    private static final Pattern LINHAS_VAZIAS_EXCESSIVAS = Pattern.compile("\\n{3,}");
    private static final Pattern NUMERO_PAGINA_ISOLADO = Pattern.compile("(?m)^\\s*\\d{1,4}\\s*$");
    private static final Pattern MARCA_SCANNER = Pattern.compile(
            "(?im)^\\s*(scanned by|digitalizado por|camscanner|adobe scan|\\[image\\]).*$");
    /** Cabeçalhos/rodapés curtos muito repetidos (≥ 3 vezes) são removidos. */
    private static final int LIMIAR_REPETICAO_CABECALHO = 3;
    private static final int MAX_LEN_CABECALHO = 80;

    public String normalizar(String textoBruto) {
        if (textoBruto == null || textoBruto.isBlank()) {
            return "";
        }
        String t = textoBruto.replace("\r\n", "\n").replace('\r', '\n');
        t = t.replace("\u000c", "\n"); // form feed
        // Normaliza ordinações OCR comuns
        t = t.replace('°', 'º');
        t = t.replace("ºo", "º").replace("oº", "º");
        t = MARCA_SCANNER.matcher(t).replaceAll("");
        t = removerCabecalhosRepetidos(t);
        t = NUMERO_PAGINA_ISOLADO.matcher(t).replaceAll("");
        t = ESPACOS_MULTIPLOS.matcher(t).replaceAll(" ");
        t = LINHAS_VAZIAS_EXCESSIVAS.matcher(t).replaceAll("\n\n");
        return t.trim();
    }

    /**
     * Normaliza página a página e reconstroi a lista (preserva números de página).
     */
    public List<PaginaTexto> normalizarPaginas(List<PaginaTexto> paginas) {
        if (paginas == null || paginas.isEmpty()) {
            return List.of();
        }
        List<PaginaTexto> out = new ArrayList<>(paginas.size());
        for (PaginaTexto p : paginas) {
            out.add(new PaginaTexto(p.numeroPagina(), normalizar(p.texto())));
        }
        return out;
    }

    private String removerCabecalhosRepetidos(String texto) {
        String[] linhas = texto.split("\n", -1);
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (String linha : linhas) {
            String key = linha.trim().toLowerCase(Locale.ROOT);
            if (key.length() > 0 && key.length() <= MAX_LEN_CABECALHO) {
                contagem.merge(key, 1, Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (String linha : linhas) {
            String key = linha.trim().toLowerCase(Locale.ROOT);
            int c = contagem.getOrDefault(key, 0);
            // Não remove linhas que parecem estrutura jurídica
            boolean estrutural = key.matches(".*(artigo|cap[ií]tulo|t[ií]tulo|sec[cç][aã]o|anexo).*");
            if (!estrutural && c >= LIMIAR_REPETICAO_CABECALHO && key.length() <= MAX_LEN_CABECALHO) {
                continue;
            }
            sb.append(linha).append('\n');
        }
        return sb.toString();
    }
}
