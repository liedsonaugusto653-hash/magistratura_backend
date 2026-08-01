package ao.magistratura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CapituloDetector {

    private static final Pattern PADRAO = Pattern.compile(
            "^\\s*(?:CAP[IÍ]TULO|T[IÍ]TULO)\\s+([IVXLCDM]+|\\d+)\\s*[-–—:]?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CASE
    );

    public List<Marco> detetar(String textoCompleto) {
        List<Marco> marcos = new ArrayList<>();
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return marcos;
        }
        Matcher m = PADRAO.matcher(textoCompleto);
        while (m.find()) {
            String rotulo = m.group(1).toUpperCase();
            String titulo = m.group(2) == null || m.group(2).isBlank() ? null : m.group(2).trim();
            marcos.add(new Marco(Marco.TipoMarco.CAPITULO, m.start(), rotulo, titulo));
        }
        return marcos;
    }
}
