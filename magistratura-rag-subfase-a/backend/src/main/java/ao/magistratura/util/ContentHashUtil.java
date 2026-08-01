package ao.magistratura.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash SHA-256 de texto normalizado (artigos e outros conteúdos derivados).
 */
public final class ContentHashUtil {

    private ContentHashUtil() {
    }

    /**
     * Normaliza whitespace e calcula SHA-256 hex do conteúdo do artigo.
     */
    public static String hashArtigo(String numero, String titulo, String texto) {
        String normalizado = normalizar(numero) + "|" + normalizar(titulo) + "|" + normalizar(texto);
        return sha256Hex(normalizado);
    }

    public static String normalizar(String t) {
        if (t == null) {
            return "";
        }
        return t.replaceAll("\\s+", " ").trim();
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
