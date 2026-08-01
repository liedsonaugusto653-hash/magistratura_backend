package ao.magistratura.service.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frente A: o parser não deve materializar linhas de índice como artigos.
 */
class EstruturaJuridicaParserTest {

    private final EstruturaJuridicaParser parser =
            new EstruturaJuridicaParser(new CapituloDetector(), new SeccaoDetector(), new RegexArtigoDetector(), new PretextoJuridicoDetector());

    @Test
    void ignoraLinhasDoIndiceEMantemCorpoDoArtigo() {
        String texto = """
                CONSTITUIÇÃO DA REPÚBLICA DE ANGOLA

                ÍNDICE

                Artigo 1.º ................................ 5
                Artigo 2.º ................................ 6

                CAPÍTULO I — Princípios fundamentais

                Artigo 1.º
                (República de Angola)
                Angola é uma República soberana e independente.

                Artigo 2.º
                (Estado democrático)
                A República de Angola é um Estado democrático de direito.
                """;

        var resultado = parser.processar(List.of(new PaginaTexto(1, texto)));
        List<ArtigoExtraido> artigos = resultado.artigos();

        assertEquals(2, artigos.size(), "deve haver só 2 artigos (corpo), não as linhas do índice");
        assertEquals("1", artigos.get(0).numero());
        assertTrue(artigos.get(0).texto().contains("República soberana"));
        assertFalse(artigos.get(0).texto().contains("...."));
        assertEquals("2", artigos.get(1).numero());
        assertTrue(artigos.get(1).texto().contains("Estado democrático"));
    }

    @Test
    void isBlocoIndiceDetectaPontosLideres() {
        assertTrue(EstruturaJuridicaParser.isBlocoIndice("Artigo 1.º ................................ 5"));
        assertFalse(EstruturaJuridicaParser.isBlocoIndice(
                "Artigo 1.º\n(República de Angola)\nAngola é uma República soberana e independente."));
    }

    @Test
    void deduplicarPrefereTextoLongo() {
        var indice = new ArtigoExtraido("1", null, "Artigo 1.º .......... 5", 1, null, null, 1, 1);
        var corpo = new ArtigoExtraido("1", "República",
                "Artigo 1.º\nAngola é uma República soberana e independente, una e indivisível.",
                2, "Capítulo I", null, 5, 5);
        var out = EstruturaJuridicaParser.deduplicarPreferindoCorpo(List.of(indice, corpo));
        assertEquals(1, out.size());
        assertTrue(out.get(0).texto().contains("soberana"));
    }

    @Test
    void ignoraPreambuloEIndiceMantemApenasCorpoNormativo() {
        String texto = """
                LEI N.º 1/20 — LEI DE BASES

                EXPOSIÇÃO DE MOTIVOS

                A presente lei visa modernizar o regime jurídico aplicável.

                ÍNDICE
                Artigo 1.º Objecto ........................ 3
                Artigo 2.º Âmbito ........................ 4

                A Assembleia Nacional decreta, nos termos da Constituição, o seguinte:

                Artigo 1.º
                (Objecto)
                A presente lei estabelece o regime jurídico geral do sector.

                Artigo 2.º
                (Âmbito)
                A presente lei aplica-se a todas as entidades públicas e privadas.
                """;

        var resultado = parser.processar(List.of(new PaginaTexto(1, texto)));
        List<ArtigoExtraido> artigos = resultado.artigos();

        assertEquals(2, artigos.size(), "preâmbulo/índice não devem virar artigos de estudo");
        assertTrue(artigos.get(0).texto().contains("regime jurídico geral"));
        assertFalse(artigos.stream().anyMatch(a -> a.texto() != null && a.texto().contains("modernizar o regime")));
    }

    @Test
    void pareceEntradaDeIndiceCurta() {
        assertTrue(EstruturaJuridicaParser.pareceEntradaDeIndiceCurta("Artigo 5.º  Direitos fundamentais    12"));
        assertFalse(EstruturaJuridicaParser.pareceEntradaDeIndiceCurta(
                "Artigo 5.º\n(Direitos fundamentais)\nSão direitos fundamentais da pessoa humana a vida, a liberdade e a segurança."));
    }


    @Test
    void ordenarPorNumeroCrescenteNaoLexicografico() {
        var a7 = new ArtigoExtraido("7", null, "Art. 7 corpo", 1, null, null, 10, 10);
        var a20 = new ArtigoExtraido("20", null, "Art. 20 corpo", 2, null, null, 20, 20);
        var a24 = new ArtigoExtraido("24", null, "Art. 24 corpo", 3, null, null, 24, 24);
        var a2 = new ArtigoExtraido("2", null, "Art. 2 corpo", 4, null, null, 2, 2);
        // Entrada fora de ordem (como OCR/índice)
        var out = EstruturaJuridicaParser.ordenarPorNumeroCrescente(List.of(a7, a20, a24, a2));
        assertEquals(List.of("2", "7", "20", "24"),
                out.stream().map(ArtigoExtraido::numero).toList());
    }

}
