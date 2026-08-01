package ao.magistratura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexArtigoDetector {

    private static final List<Pattern> PADROES = List.of(
            Pattern.compile(
                    "^\\s*Art(?:igo|\\.)\\s+(\\d{1,4})\\s*\\.?\\s*[º°o]?\\s*\\(([^)]+)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CASE),
            Pattern.compile(
                    "^\\s*Art(?:igo|\\.)\\s+(\\d{1,4})\\s*\\.?\\s*[º°o]?\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CASE)
    );

    public List<Marco> detetar(String textoCompleto) {
        List<Marco> marcos = new ArrayList<>();
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return marcos;
        }
        boolean[] ocupado = new boolean[textoCompleto.length() + 1];
        for (Pattern padrao : PADROES) {
            Matcher m = padrao.matcher(textoCompleto);
            while (m.find()) {
                if (ocupado[m.start()]) {
                    continue;
                }
                String numero = m.group(1);
                String titulo = m.groupCount() >= 2 ? m.group(2) : null;
                if (titulo != null) {
                    titulo = titulo.trim();
                    if (titulo.isEmpty()) {
                        titulo = null;
                    }
                }
                marcos.add(new Marco(Marco.TipoMarco.ARTIGO, m.start(), numero, titulo));
                ocupado[m.start()] = true;
            }
        }
        marcos.sort((a, b) -> Integer.compare(a.posicao(), b.posicao()));
        return marcos;
    }
}
