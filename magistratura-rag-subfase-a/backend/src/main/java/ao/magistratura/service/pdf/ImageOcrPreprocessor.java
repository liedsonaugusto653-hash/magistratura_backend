package ao.magistratura.service.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

/**
 * Pré-processamento leve (só JDK) antes do Tesseract — pensado para scans
 * legislativos (Diário da República, acórdãos, decretos em PDF-imagem).
 * <p>
 * Pipeline: escala opcional → cinzento → contraste → Otsu (binarização).
 * Não depende de OpenCV.
 */
public final class ImageOcrPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(ImageOcrPreprocessor.class);

    /** Largura mínima desejada (px); abaixo disto faz upscale 2×. */
    private static final int MIN_WIDTH_PX = 1200;

    private ImageOcrPreprocessor() {
    }

    /**
     * Prepara a imagem rasterizada para OCR.
     *
     * @param source   página renderizada pelo PDFBox
     * @param binarizar se true aplica limiar de Otsu (melhor em scans limpos);
     *                  se false fica em cinzentos com contraste (melhor em páginas sujas/sombreadas)
     */
    public static BufferedImage preparar(BufferedImage source, boolean binarizar) {
        if (source == null) {
            return null;
        }
        BufferedImage img = source;

        if (img.getWidth() < MIN_WIDTH_PX) {
            img = escalar(img, 2.0);
            log.debug("OCR preprocess: upscale 2× → {}x{}", img.getWidth(), img.getHeight());
        }

        img = paraCinzento(img);
        img = reforcarContraste(img, 1.25f, 5f);

        if (binarizar) {
            img = otsuBinarizar(img);
        }
        return img;
    }

    /** Tenta primeiro versão cinzenta; se o texto for curto, o caller pode pedir binarizada. */
    public static BufferedImage prepararPadrao(BufferedImage source) {
        return preparar(source, false);
    }

    public static BufferedImage prepararBinario(BufferedImage source) {
        return preparar(source, true);
    }

    // ------------------------------------------------------------------

    static BufferedImage paraCinzento(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    static BufferedImage reforcarContraste(BufferedImage gray, float scale, float offset) {
        try {
            RescaleOp op = new RescaleOp(scale, offset, null);
            return op.filter(gray, null);
        } catch (Exception e) {
            return gray;
        }
    }

    static BufferedImage escalar(BufferedImage src, double factor) {
        int w = Math.max(1, (int) Math.round(src.getWidth() * factor));
        int h = Math.max(1, (int) Math.round(src.getHeight() * factor));
        BufferedImage out = new BufferedImage(w, h, src.getType() == 0 ? BufferedImage.TYPE_INT_RGB : src.getType());
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Binarização global de Otsu — eficaz em páginas com fundo claro e tipografia escura
     * (tipicamente scans de Diário Oficial).
     */
    static BufferedImage otsuBinarizar(BufferedImage gray) {
        int w = gray.getWidth();
        int h = gray.getHeight();
        int[] hist = new int[256];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = gray.getRGB(x, y);
                int lum = (rgb >> 16) & 0xff; // já é cinzento
                hist[lum]++;
            }
        }
        int total = w * h;
        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * hist[i];
        }
        float sumB = 0;
        int wB = 0;
        float maxVar = -1;
        int threshold = 128;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) {
                continue;
            }
            int wF = total - wB;
            if (wF == 0) {
                break;
            }
            sumB += (float) t * hist[t];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);
            if (varBetween > maxVar) {
                maxVar = varBetween;
                threshold = t;
            }
        }

        BufferedImage binary = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int lum = (gray.getRGB(x, y) >> 16) & 0xff;
                int v = lum > threshold ? 0xFFFFFF : 0x000000;
                binary.setRGB(x, y, v);
            }
        }
        return binary;
    }
}
