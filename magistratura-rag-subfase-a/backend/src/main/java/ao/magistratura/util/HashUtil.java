package ao.magistratura.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cálculo de hash SHA-256 de ficheiros, usado para detetar PDFs duplicados
 * na importação para a Biblioteca Jurídica.
 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256(File ficheiro) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(ficheiro)) {
                byte[] buffer = new byte[8192];
                int lidos;
                while ((lidos = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, lidos);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 está sempre disponível na JVM; nunca deve acontecer.
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }
}
