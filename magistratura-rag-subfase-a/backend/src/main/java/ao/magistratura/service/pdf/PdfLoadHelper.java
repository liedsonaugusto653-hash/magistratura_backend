package ao.magistratura.service.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Abertura de PDFs com timeout e inspecção de permissões.
 * Evita hangs indefinidos em PDFs protegidos/corruptos no PDFBox.
 */
public final class PdfLoadHelper {

    private static final Logger log = LoggerFactory.getLogger(PdfLoadHelper.class);

    private PdfLoadHelper() {
    }

    /**
     * Abre o PDF com timeout. Se ultrapassar {@code timeoutSeconds}, cancela
     * a tentativa e lança {@link IOException} controlada.
     */
    public static PDDocument loadComTimeout(File ficheiro, int timeoutSeconds) throws IOException {
        if (timeoutSeconds <= 0) {
            return loadDirecto(ficheiro);
        }
        ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pdf-load");
            t.setDaemon(true);
            return t;
        });
        AtomicReference<PDDocument> ref = new AtomicReference<>();
        Future<PDDocument> future = pool.submit(() -> {
            PDDocument doc = loadDirecto(ficheiro);
            ref.set(doc);
            return doc;
        });
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            PDDocument partial = ref.get();
            if (partial != null) {
                try {
                    partial.close();
                } catch (Exception ignored) {
                }
            }
            throw new IOException(
                    "Timeout ao abrir o PDF (" + timeoutSeconds
                            + "s). O ficheiro pode estar protegido, corrompido ou ser demasiado complexo.",
                    e);
        } catch (ExecutionException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            if (c instanceof InvalidPasswordException) {
                throw new IOException(
                        "O PDF está protegido por password e não pode ser aberto sem a palavra-passe.", c);
            }
            if (c instanceof IOException io) {
                throw io;
            }
            throw new IOException("Falha ao abrir o PDF: " + c.getMessage(), c);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abertura do PDF interrompida.", e);
        } finally {
            pool.shutdownNow();
        }
    }

    private static PDDocument loadDirecto(File ficheiro) throws IOException {
        try {
            return Loader.loadPDF(ficheiro);
        } catch (InvalidPasswordException e) {
            throw new IOException(
                    "O PDF está protegido por password e não pode ser aberto sem a palavra-passe.", e);
        }
    }

    /**
     * Executa trabalho sobre um documento já aberto, com timeout global.
     * O caller mantém a responsabilidade de fechar o documento.
     */
    public static <T> T comTimeout(String operacao, int timeoutSeconds, java.util.concurrent.Callable<T> trabalho)
            throws IOException {
        if (timeoutSeconds <= 0) {
            try {
                return trabalho.call();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(operacao + " falhou: " + e.getMessage(), e);
            }
        }
        ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pdf-op-" + operacao.replaceAll("\\s+", "-"));
            t.setDaemon(true);
            return t;
        });
        Future<T> future = pool.submit(trabalho);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IOException(
                    "Timeout em «" + operacao + "» (" + timeoutSeconds
                            + "s). PDF possivelmente protegido ou ilegível.",
                    e);
        } catch (ExecutionException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            if (c instanceof IOException io) {
                throw io;
            }
            throw new IOException(operacao + " falhou: " + c.getMessage(), c);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(operacao + " interrompida.", e);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Inspecciona encriptação e permissões de extracção/impressão (OCR precisa de rasterizar).
     */
    public static PermissoesPdf inspeccionar(PDDocument doc) {
        boolean encriptado = doc.isEncrypted();
        AccessPermission ap = doc.getCurrentAccessPermission();
        boolean podeExtrair = ap == null || ap.canExtractContent();
        boolean podeImprimir = ap == null || ap.canPrint();
        // PDFBox por vezes reporta isEncrypted=true só com restrições de owner password
        // (aberto sem user password). OCR precisa de render → print/extract.
        boolean bloqueado = encriptado && !podeExtrair && !podeImprimir;
        return new PermissoesPdf(encriptado, podeExtrair, podeImprimir, bloqueado);
    }

    public record PermissoesPdf(
            boolean encriptado,
            boolean podeExtrairTexto,
            boolean podeImprimir,
            /** True se não há forma razoável de obter texto (nem nativo nem OCR). */
            boolean bloqueadoTotalmente
    ) {
        /**
         * Código estável para a API/UI (não depende da língua da mensagem).
         * null se não houver restrição relevante.
         */
        public String codigoErro() {
            if (!encriptado) {
                return null;
            }
            if (bloqueadoTotalmente) {
                return "PDF_PROTECTED_BLOCKED";
            }
            if (!podeExtrairTexto) {
                return "PDF_PROTECTED_NO_EXTRACT";
            }
            return "PDF_RESTRICTED";
        }

        public String mensagemUtilizador() {
            if (!encriptado) {
                return null;
            }
            if (bloqueadoTotalmente) {
                return "PDF protegido/com restrições e sem texto seleccionável. "
                        + "Exporte uma versão sem protecção (ou «Imprimir para PDF») e volte a importar.";
            }
            if (!podeExtrairTexto) {
                return "Este PDF restringe a cópia de texto. "
                        + "Se a extracção nativa falhar, tente exportar sem protecção («Imprimir para PDF»).";
            }
            return "PDF com restrições — a extracção pode ser limitada. "
                    + "Se falhar, exporte uma versão sem protecção.";
        }
    }
}
