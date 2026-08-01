package ao.magistratura.service.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDF digital com texto + índice + TÍTULO (padrão CRA).
 */
class EstruturaJuridicaParserPipeline2Test {

    private final EstruturaJuridicaParser parser =
            new EstruturaJuridicaParser(new CapituloDetector(), new SeccaoDetector(), new RegexArtigoDetector(), new PretextoJuridicoDetector());

    @Test
    void pdfDigitalComTextoProduzArtigos() {
        String texto = """
                CONSTITUIÇÃO DA REPÚBLICA DE ANGOLA

                ÍNDICE
                Artigo 1.º ................................ 5
                Artigo 2.º ................................ 6

                TÍTULO I — Princípios fundamentais

                Artigo 1.º
                (República de Angola)
                Angola é uma República soberana e independente, una e indivisível,
                que tem como objectivo fundamental a construção de uma sociedade livre.

                Artigo 2.º
                (Estado democrático)
                A República de Angola é um Estado democrático de direito.
                """;
        var resultado = parser.processar(List.of(new PaginaTexto(1, texto)));
        assertTrue(resultado.artigos().size() >= 2, "esperado >=2 artigos, got " + resultado.artigos().size());
        assertEquals("1", resultado.artigos().get(0).numero());
        assertTrue(resultado.artigos().get(0).texto().contains("soberana"));
    }

    @Test
    void textoVazioProduzZeroArtigos() {
        var resultado = parser.processar(List.of(new PaginaTexto(1, "\u000c\u000c")));
        assertEquals(0, resultado.artigos().size());
    }
}
