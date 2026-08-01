package ao.magistratura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deteta cabeçalhos de secção no texto bruto do PDF.
 * Suporta "SECÇÃO I", "Secção 1", "SECCAO I" (sem cedilha, comum em OCR/scans).
 */
@Component
public class SeccaoDetector {

    private static final Pattern PADRAO = Pattern.compile(
            "^\\s*SEC[CÇ][AÃ]O\\s+([IVXLCDM]+|\\d+)\\s*[-–—:]?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    public List<Marco> detetar(String textoCompleto) {
        List<Marco> marcos = new ArrayList<>();
        Matcher m = PADRAO.matcher(textoCompleto);
        while (m.find()) {
            String rotulo = m.group(1).toUpperCase();
            String titulo = m.group(2) == null || m.group(2).isBlank() ? null : m.group(2).trim();
            marcos.add(new Marco(Marco.TipoMarco.SECCAO, m.start(), rotulo, titulo));
        }
        return marcos;
    }
}
