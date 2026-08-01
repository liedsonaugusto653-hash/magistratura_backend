package ao.magistratura.service.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextoJuridicoNormalizerTest {

    private final TextoJuridicoNormalizer normalizer = new TextoJuridicoNormalizer();

    @Test
    void removeNumerosDePaginaIsoladosEEspacos() {
        String in = "Artigo 1.º\n\n\n12\n\nAngola é uma República.\n";
        String out = normalizer.normalizar(in);
        assertTrue(out.contains("Artigo 1.º"));
        assertTrue(out.contains("República"));
        assertFalse(out.contains("\n12\n"));
    }

    @Test
    void normalizaOrdinalGrauParaMasculino() {
        String out = normalizer.normalizar("Artigo 1.°\nTexto");
        assertTrue(out.contains("º"));
    }
}
