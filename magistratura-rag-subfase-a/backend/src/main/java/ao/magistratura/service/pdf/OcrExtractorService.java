package ao.magistratura.service.pdf;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

/**
 * OCR determinístico via Tesseract (Tess4J) para PDFs sem camada de texto
 * (scans do Diário da República, acórdãos, decretos em imagem).
 * <p>
 * Melhorias: pré-processamento de imagem, 2.ª tentativa Otsu, OCR selectivo
 * de páginas (híbridos), DPI/PSM configuráveis.
 */
@Service
public class OcrExtractorService {

    private static final Logger log = LoggerFactory.getLogger(OcrExtractorService.class);

    @Value("${app.pipeline.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${app.pipeline.ocr.language:por}")
    private String language;

    @Value("${app.pipeline.ocr.datapath:}")
    private String datapath;

    @Value("${app.pipeline.ocr.dpi:250}")
    private int dpi;

    @Value("${app.pipeline.ocr.max-pages:0}")
    private int maxPages;

    @Value("${app.pipeline.ocr.render-timeout-seconds:30}")
    private int renderTimeoutSeconds;

    @Value("${app.pipeline.ocr.psm:6}")
    private int pageSegMode;

    @Value("${app.pipeline.ocr.preprocess:true}")
    private boolean preprocessEnabled;

    @Value("${app.pipeline.ocr.retry-binarize-below-chars:30}")
    private int retryBinarizeBelowChars;

    public boolean isDisponivel() {
        if (!ocrEnabled) {
            return false;
        }
        try {
            ITesseract t = criarTesseract();
            if (t == null) {
                return false;
            }
            // Smoke test: se tessdata/idioma faltarem, falha já — evita hang no doOCR.
            java.awt.image.BufferedImage img =
                    new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
            t.doOCR(img);
            return true;
        } catch (Exception e) {
            log.warn("OCR indisponível (Tesseract/tessdata/idioma): {}", e.getMessage());
            return false;
        }
    }

    public List<PaginaTexto> extrairPorPagina(File ficheiro) throws IOException {
        return extrairPorPagina(ficheiro, null);
    }

    public List<PaginaTexto> extrairPorPagina(File ficheiro, BiConsumer<Integer, Integer> onProgress)
            throws IOException {
        return extrairPorPagina(ficheiro, null, onProgress);
    }

    /**
     * @param soPaginas1Based se não-nulo/não-vazio, só estas páginas (1-indexadas) sofrem OCR
     */
    public List<PaginaTexto> extrairPorPagina(File ficheiro,
                                              Set<Integer> soPaginas1Based,
                                              BiConsumer<Integer, Integer> onProgress)
            throws IOException {
        if (!ocrEnabled) {
            throw new IOException("OCR desactivado (app.pipeline.ocr.enabled=false).");
        }

        List<PaginaTexto> paginas = new ArrayList<>();
        ITesseract tesseract = criarTesseract();

        try (PDDocument documento = PdfLoadHelper.loadComTimeout(ficheiro, 20)) {
            PdfLoadHelper.PermissoesPdf perm = PdfLoadHelper.inspeccionar(documento);
            if (perm.bloqueadoTotalmente()
                    || (perm.encriptado() && !perm.podeImprimir() && !perm.podeExtrairTexto())) {
                throw new IOException(perm.mensagemUtilizador() != null
                        ? perm.mensagemUtilizador()
                        : "PDF protegido: não é possível rasterizar páginas para OCR. "
                          + "Exporte com «Imprimir para PDF» e volte a importar.");
            }
            if (perm.encriptado() && !perm.podeExtrairTexto() && perm.podeImprimir()) {
                log.info("OCR em PDF com restrição de cópia mas print permitido — a rasterizar.");
            }

            PDFRenderer renderer = new PDFRenderer(documento);
            renderer.setSubsamplingAllowed(true);

            int total = documento.getNumberOfPages();
            int limite = maxPages > 0 ? Math.min(total, maxPages) : total;
            Set<Integer> alvo = (soPaginas1Based == null || soPaginas1Based.isEmpty())
                    ? null
                    : soPaginas1Based;

            int aProcessar = alvo == null
                    ? limite
                    : (int) alvo.stream().filter(p -> p >= 1 && p <= limite).count();

            log.info("OCR a iniciar: {} páginas no PDF, a processar≈{} dpi={} lang={} preprocess={} ficheiro={}",
                    total, aProcessar, dpi, language, preprocessEnabled, ficheiro.getName());

            int falhasRaster = 0;
            int processadas = 0;

            for (int i = 0; i < limite; i++) {
                int numPagina = i + 1;
                if (alvo != null && !alvo.contains(numPagina)) {
                    paginas.add(new PaginaTexto(numPagina, ""));
                    continue;
                }

                final int pageIndex = i;
                BufferedImage image;
                try {
                    Callable<BufferedImage> renderTask =
                            () -> renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
                    image = PdfLoadHelper.comTimeout(
                            "rasterizar página " + numPagina,
                            renderTimeoutSeconds,
                            renderTask);
                } catch (IOException e) {
                    falhasRaster++;
                    log.error("Rasterização falhou/timeout página {}: {}", numPagina, e.getMessage());
                    paginas.add(new PaginaTexto(numPagina, ""));
                    processadas++;
                    if (onProgress != null) {
                        onProgress.accept(processadas, Math.max(aProcessar, 1));
                    }
                    if (falhasRaster >= 3 && pageIndex < 5) {
                        throw new IOException(
                                "Falha ao rasterizar as primeiras páginas do PDF (timeout ou protecção). "
                                        + e.getMessage(), e);
                    }
                    continue;
                }

                String texto = ocrComPreprocessamento(tesseract, image, numPagina);
                paginas.add(new PaginaTexto(numPagina, texto));
                processadas++;

                if (onProgress != null) {
                    onProgress.accept(processadas, Math.max(aProcessar, 1));
                }
                if (processadas % 5 == 0 || processadas == aProcessar) {
                    log.info("OCR progresso: {}/{} (última pág. {} → {} chars)",
                            processadas, aProcessar, numPagina,
                            texto != null ? texto.trim().length() : 0);
                }
            }

            for (int i = limite; i < total; i++) {
                paginas.add(new PaginaTexto(i + 1, ""));
            }
        }

        long chars = paginas.stream()
                .mapToLong(p -> p.texto() != null ? p.texto().trim().length() : 0)
                .sum();
        log.info("OCR concluído: {} páginas, ~{} chars", paginas.size(), chars);
        return paginas;
    }

    public String extrairPagina(File ficheiro, int numeroPagina1Based) throws IOException {
        Set<Integer> so = new HashSet<>();
        so.add(numeroPagina1Based);
        List<PaginaTexto> lista = extrairPorPagina(ficheiro, so, null);
        for (PaginaTexto p : lista) {
            if (p.numeroPagina() == numeroPagina1Based) {
                return p.texto() != null ? p.texto() : "";
            }
        }
        return "";
    }

    private String ocrComPreprocessamento(ITesseract tesseract, BufferedImage original, int numPagina) {
        BufferedImage preparada = preprocessEnabled
                ? ImageOcrPreprocessor.prepararPadrao(original)
                : original;

        String texto = doOcrSafe(tesseract, preparada, numPagina);
        int len = texto.trim().length();

        if (preprocessEnabled && len < retryBinarizeBelowChars) {
            log.debug("Pág. {}: OCR fraco ({} chars) — a tentar binarização Otsu", numPagina, len);
            BufferedImage bin = ImageOcrPreprocessor.prepararBinario(original);
            String texto2 = doOcrSafe(tesseract, bin, numPagina);
            if (texto2.trim().length() > len) {
                texto = texto2;
                log.debug("Pág. {}: Otsu melhorou {} → {} chars", numPagina, len, texto.trim().length());
            }
        }
        return texto;
    }

    private String doOcrSafe(ITesseract tesseract, BufferedImage image, int numPagina) {
        try {
            String texto = tesseract.doOCR(image);
            return texto != null ? texto : "";
        } catch (TesseractException e) {
            log.error("OCR falhou na página {}: {}", numPagina, e.getMessage());
            return "";
        }
    }

    private ITesseract criarTesseract() {
        Tesseract tesseract = new Tesseract();
        String path = resolverDatapath();
        if (path != null) {
            tesseract.setDatapath(path);
            log.debug("Tesseract datapath={}", path);
        }
        tesseract.setLanguage(language != null && !language.isBlank() ? language : "por");
        tesseract.setPageSegMode(pageSegMode > 0 ? pageSegMode : 6);
        try {
            tesseract.setOcrEngineMode(1);
        } catch (Exception ignored) {
        }
        tesseract.setVariable("user_defined_dpi", String.valueOf(dpi));
        tesseract.setVariable("preserve_interword_spaces", "1");
        return tesseract;
    }

    /**
     * Resolve tessdata: config → pastas típicas Windows/Linux → null (Tess4J auto).
     */
    private String resolverDatapath() {
        if (datapath != null && !datapath.isBlank()) {
            File d = new File(datapath.trim());
            if (d.isDirectory()) {
                return d.getAbsolutePath();
            }
            log.warn("PIPELINE_OCR_DATAPATH inválido: {}", datapath);
        }
        String[] candidatos = {
                "C:/Program Files/Tesseract-OCR/tessdata",
                "C:/Program Files (x86)/Tesseract-OCR/tessdata",
                System.getenv("TESSDATA_PREFIX") != null ? System.getenv("TESSDATA_PREFIX") : "",
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tessdata"
        };
        for (String c : candidatos) {
            if (c == null || c.isBlank()) continue;
            File d = new File(c);
            if (d.isDirectory() && new File(d, "por.traineddata").isFile()) {
                return d.getAbsolutePath();
            }
            if (d.isDirectory()) {
                return d.getAbsolutePath();
            }
        }
        return null;
    }
}
