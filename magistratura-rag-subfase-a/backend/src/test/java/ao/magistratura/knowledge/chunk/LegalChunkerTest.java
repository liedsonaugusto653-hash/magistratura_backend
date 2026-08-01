package ao.magistratura.knowledge.chunk;

import ao.magistratura.entity.Artigo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LegalChunkerTest {

    private final LegalChunker chunker = new LegalChunker();

    @Test
    void artigoCurto_umUnicoChunk() {
        Artigo a = Artigo.builder()
                .id(UUID.randomUUID())
                .numero("1")
                .titulo("República")
                .texto("Angola é uma República soberana.")
                .ordem(1)
                .build();
        List<LegalChunker.Chunk> chunks = chunker.chunkArtigo(a);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).texto().contains("Artigo 1"));
        assertTrue(chunks.get(0).texto().contains("República soberana"));
        assertNotNull(chunks.get(0).hash());
    }

    @Test
    void artigoLongo_multiplosChunks_preservaConteudo() {
        // i <= 60 garante texto claramente acima de MAX_CHARS (3500).
        // Não alterar MAX_CHARS de produção — está calibrado para embeddings.
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 60; i++) {
            sb.append(i).append(") Disposição normativa de teste número ").append(i)
              .append(" com texto suficiente para forçar divisão. ");
        }
        Artigo a = Artigo.builder()
                .id(UUID.randomUUID())
                .numero("21")
                .titulo("Direitos fundamentais")
                .texto(sb.toString())
                .ordem(21)
                .build();
        List<LegalChunker.Chunk> chunks = chunker.chunkArtigo(a);
        assertTrue(chunks.size() >= 2, "esperado vários chunks, obteve " + chunks.size());
        String joined = chunks.stream().map(LegalChunker.Chunk::texto).reduce("", String::concat);
        assertTrue(joined.contains("Artigo 21"));
        assertTrue(joined.contains("Direitos fundamentais"));
    }

    @Test
    void textoVazio_listaVazia() {
        Artigo a = Artigo.builder().id(UUID.randomUUID()).numero("9").texto("").ordem(9).build();
        assertTrue(chunker.chunkArtigo(a).isEmpty());
    }

    @Test
    void textoNull_listaVazia() {
        Artigo a = Artigo.builder().id(UUID.randomUUID()).numero("10").texto(null).ordem(10).build();
        assertTrue(chunker.chunkArtigo(a).isEmpty());
    }
}
