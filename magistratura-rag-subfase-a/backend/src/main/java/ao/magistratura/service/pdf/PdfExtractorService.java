package ao.magistratura.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracção de texto nativo via Apache PDFBox (sem OCR).
 */
@Component
public class PdfExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractorService.class);

    @Value("${app.pipeline.pdf.open-timeout-seconds:45}")
    private int openTimeoutSeconds;

    public List<PaginaTexto> extrairPorPagina(File ficheiro) throws IOException {
        List<PaginaTexto> paginas = new ArrayList<>();

        try (PDDocument documento = PdfLoadHelper.loadComTimeout(ficheiro, Math.min(openTimeoutSeconds, 30))) {
            PdfLoadHelper.PermissoesPdf perm = PdfLoadHelper.inspeccionar(documento);
            if (perm.encriptado()) {
                log.warn("PDF '{}' encriptado extract={} print={}",
                        ficheiro.getName(), perm.podeExtrairTexto(), perm.podeImprimir());
            }

            int totalPaginas = documento.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int totalChars = 0;
            for (int i = 1; i <= totalPaginas; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String texto;
                try {
                    texto = stripper.getText(documento);
                } catch (Exception e) {
                    log.warn("PDFBox stripper página {}: {}", i, e.getMessage());
                    texto = "";
                }
                if (texto == null) {
                    texto = "";
                }
                totalChars += texto.trim().length();
                paginas.add(new PaginaTexto(i, texto));
            }

            log.info("PDFBox '{}': {} páginas, ~{} chars", ficheiro.getName(), totalPaginas, totalChars);
            if (totalChars == 0 && totalPaginas > 0) {
                log.warn("PDF '{}' devolveu 0 caracteres em {} páginas (scan ou restrição).",
                        ficheiro.getName(), totalPaginas);
            }
        } catch (IOException e) {
            if (e.getCause() instanceof InvalidPasswordException
                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("password"))) {
                throw new IOException("O PDF está protegido por password e não pode ser lido.", e);
            }
            log.error("Falha ao extrair texto do PDF {}: {}", ficheiro.getName(), e.getMessage());
            throw e;
        }

        return paginas;
    }

    public int contarPaginas(File ficheiro) throws IOException {
        try (PDDocument documento = PdfLoadHelper.loadComTimeout(ficheiro, 15)) {
            return documento.getNumberOfPages();
        }
    }
}
