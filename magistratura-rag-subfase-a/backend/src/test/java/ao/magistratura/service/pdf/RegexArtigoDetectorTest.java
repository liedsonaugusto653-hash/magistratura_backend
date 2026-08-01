package ao.magistratura.service.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegexArtigoDetectorTest {

    private final RegexArtigoDetector detector = new RegexArtigoDetector();

    @Test
    void reconheceVariantesAngolanas() {
        String texto = """
                ARTIGO 1.º
                (República)
                Texto um.

                Artigo 2º
                Texto dois.

                Art. 3.º
                Texto três.

                ARTIGO 4
                Texto quatro.
                """;
        List<Marco> marcos = detector.detetar(texto);
        assertEquals(4, marcos.size());
        assertEquals("1", marcos.get(0).rotulo());
        assertEquals("2", marcos.get(1).rotulo());
        assertEquals("3", marcos.get(2).rotulo());
        assertEquals("4", marcos.get(3).rotulo());
    }
}
