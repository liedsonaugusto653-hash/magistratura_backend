package ao.magistratura.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * Classifica o PDF antes da extracção completa: TEXT / IMAGE / HYBRID / PROTECTED.
 * Usa amostragem de páginas e timeout na abertura para não travar em PDFs protegidos.
 */
@Service
public class PdfAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PdfAnalysisService.class);

    @Value("${app.pipeline.ocr.min-chars-uteis:40}")
    private int minCharsUteis;

    @Value("${app.pipeline.ocr.min-chars-por-pagina:15}")
    private int minCharsPorPagina;

    /** Timeout total para abrir + amostrar o PDF (segundos). 0 = sem timeout. */
    @Value("${app.pipeline.pdf.open-timeout-seconds:45}")
    private int openTimeoutSeconds;

    public PdfAnalysisResult analisar(File ficheiro) throws IOException {
        PdfAnalysisResult r = new PdfAnalysisResult();
        r.setTamanhoBytes(ficheiro.length());
        return PdfLoadHelper.comTimeout("análise PDF", openTimeoutSeconds, () -> analisarInterno(ficheiro, r));
    }

    private PdfAnalysisResult analisarInterno(File ficheiro, PdfAnalysisResult r) throws IOException {
        try (PDDocument doc = PdfLoadHelper.loadComTimeout(ficheiro, 0)) {  // timeout já no comTimeout exterior
            PdfLoadHelper.PermissoesPdf perm = PdfLoadHelper.inspeccionar(doc);
            r.setEncriptado(perm.encriptado());
            if (perm.encriptado()) {
                String msg = perm.mensagemUtilizador();
                if (msg != null) {
                    r.addAviso(msg);
                }
            }

            if (perm.bloqueadoTotalmente()) {
                r.setTipo(PdfTipo.PROTECTED);
                r.setOcrNecessario(false);
                r.setPaginas(doc.getNumberOfPages());
                r.setMotivo(perm.mensagemUtilizador());
                r.setCodigoErro(perm.codigoErro());
                log.warn("PDF '{}' bloqueado por permissões (encriptado, sem extract/print)", ficheiro.getName());
                return r;
            }

            int total = doc.getNumberOfPages();
            r.setPaginas(total);
            if (total == 0) {
                r.setTipo(PdfTipo.UNKNOWN);
                r.setOcrNecessario(false);
                r.setMotivo("PDF sem páginas.");
                return r;
            }

            int amostra = Math.min(total, 8);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            long chars = 0;
            int comTexto = 0;
            int comImagem = 0;

            for (int i = 0; i < amostra; i++) {
                int pageIdx = amostra == 1 ? 1 : 1 + (i * (total - 1) / (amostra - 1));
                stripper.setStartPage(pageIdx);
                stripper.setEndPage(pageIdx);
                String texto;
                try {
                    texto = stripper.getText(doc);
                } catch (Exception e) {
                    log.warn("Stripper falhou na página {}: {}", pageIdx, e.getMessage());
                    texto = "";
                }
                String limpo = texto == null ? "" : texto.replace("\u000c", "").trim();
                chars += limpo.length();
                if (limpo.length() >= minCharsPorPagina) {
                    comTexto++;
                }
                if (paginaTemImagem(doc, pageIdx - 1)) {
                    comImagem++;
                }
            }

            long charsEstimados = amostra > 0 ? (chars * total) / amostra : 0;
            r.setCharsTextoNativo(charsEstimados);
            r.setPaginasComTexto(comTexto);
            r.setPaginasComImagem(comImagem);

            if (charsEstimados < minCharsUteis) {
                // Diário da República / Jurisnet: muitas vezes encriptados com copy:no
                // mas print:yes e páginas inteiras em imagem. Nesse caso OCR é viável.
                // Só marcar PROTECTED (sem OCR) quando nem extract nem print são possíveis.
                if (perm.bloqueadoTotalmente() || (perm.encriptado() && !perm.podeImprimir() && !perm.podeExtrairTexto())) {
                    r.setTipo(PdfTipo.PROTECTED);
                    r.setOcrNecessario(false);
                    r.setCodigoErro(perm.codigoErro() != null ? perm.codigoErro() : "PDF_PROTECTED_BLOCKED");
                    r.setMotivo(
                            "PDF protegido sem permissão de extracção nem impressão. "
                            + "Exporte uma versão sem protecção (ou «Imprimir para PDF») e volte a importar.");
                } else if (perm.encriptado() && !perm.podeExtrairTexto() && perm.podeImprimir()) {
                    // Caso típico dos PDF oficiais angolanos: scan + restrição de cópia, print OK
                    r.setTipo(PdfTipo.IMAGE);
                    r.setOcrNecessario(true);
                    r.setCodigoErro(null);
                    r.setMotivo(
                            "PDF oficial com páginas em imagem e restrição de cópia — "
                            + "será processado por OCR (impressão/rasterização permitida).");
                    r.addAviso("Restrição de cópia detectada; a usar OCR em vez de extracção nativa.");
                } else {
                    r.setTipo(PdfTipo.IMAGE);
                    r.setOcrNecessario(true);
                    r.setMotivo("Texto nativo insuficiente (scan/imagem) — OCR necessário.");
                }
            } else if (comTexto < amostra / 2 && comImagem > 0) {
                r.setTipo(PdfTipo.HYBRID);
                r.setOcrNecessario(true);
                r.setMotivo("Documento híbrido: parte das páginas sem texto seleccionável.");
                r.addAviso("OCR será usado para complementar páginas sem texto.");
            } else {
                r.setTipo(PdfTipo.TEXT);
                r.setOcrNecessario(false);
                r.setMotivo("Camada de texto suficiente para extracção directa.");
            }

            log.info("Análise PDF '{}': tipo={} páginas={} chars~{} ocr={} enc={} extract={} print={}",
                    ficheiro.getName(), r.getTipo(), total, charsEstimados, r.isOcrNecessario(),
                    perm.encriptado(), perm.podeExtrairTexto(), perm.podeImprimir());
            return r;
        }
    }

    private boolean paginaTemImagem(PDDocument doc, int pageIndexZeroBased) {
        try {
            PDPage page = doc.getPage(pageIndexZeroBased);
            PDResources resources = page.getResources();
            if (resources == null) {
                return false;
            }
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDImageXObject) {
                    return true;
                }
            }
        } catch (Exception e) {
            // best-effort
        }
        return false;
    }
}
