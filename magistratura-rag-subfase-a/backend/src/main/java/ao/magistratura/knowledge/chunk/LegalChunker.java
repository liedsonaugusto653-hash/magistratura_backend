package ao.magistratura.knowledge.chunk;

import ao.magistratura.entity.Artigo;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LegalChunker {

    private static final int MAX_CHARS = 3500;
    private static final int OVERLAP = 400;
    private static final Pattern QUEBRA = Pattern.compile(
            "(?m)(?=^\\s*(?:\\d+[.)]|[a-z]\\)|n\\.?º\\s*\\d+|Art(?:igo|\\.)\\s+\\d+))");

    public record Chunk(UUID id, int indice, String texto, String hash) {}

    public List<Chunk> chunkArtigo(Artigo artigo) {
        // Verificar o corpo isoladamente ANTES de concatenar o cabeçalho.
        // Caso contrário um artigo sem texto gerava chunk "fantasma" só com "Artigo N".
        String body = artigo.getTexto() != null ? artigo.getTexto().trim() : "";
        if (body.isEmpty()) {
            return List.of();
        }

        String header = "Artigo " + (artigo.getNumero() != null ? artigo.getNumero() : "?");
        if (artigo.getTitulo() != null && !artigo.getTitulo().isBlank()) {
            header += " — " + artigo.getTitulo();
        }
        String base = (header + "\n" + body).trim();

        List<String> pieces = split(base);
        List<Chunk> out = new ArrayList<>();
        int idx = 0;
        for (String p : pieces) {
            String h = sha256(p);
            UUID id = UUID.nameUUIDFromBytes(
                    (artigo.getId() + ":" + idx + ":" + h).getBytes(StandardCharsets.UTF_8));
            out.add(new Chunk(id, idx++, p, h));
        }
        return out;
    }

    private List<String> split(String base) {
        if (base.length() <= MAX_CHARS) {
            return List.of(base);
        }
        String[] parts = QUEBRA.split(base);
        List<String> blocos = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (cur.length() + part.length() > MAX_CHARS && !cur.isEmpty()) {
                blocos.add(cur.toString().trim());
                String tail = cur.substring(Math.max(0, cur.length() - OVERLAP));
                cur = new StringBuilder(tail);
            }
            cur.append(part);
        }
        if (!cur.isEmpty()) {
            blocos.add(cur.toString().trim());
        }

        List<String> finalList = new ArrayList<>();
        for (String b : blocos) {
            if (b.length() <= MAX_CHARS) {
                finalList.add(b);
            } else {
                for (int i = 0; i < b.length(); i += MAX_CHARS - OVERLAP) {
                    finalList.add(b.substring(i, Math.min(b.length(), i + MAX_CHARS)));
                }
            }
        }
        return finalList;
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
