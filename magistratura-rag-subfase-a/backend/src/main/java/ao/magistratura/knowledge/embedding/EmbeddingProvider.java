package ao.magistratura.knowledge.embedding;

import java.util.ArrayList;
import java.util.List;

public interface EmbeddingProvider {
    String nome();
    String modelo();
    int dimensoes();
    float[] embed(String texto);

    default List<float[]> embedBatch(List<String> textos) {
        List<float[]> out = new ArrayList<>(textos.size());
        for (String t : textos) {
            out.add(embed(t));
        }
        return out;
    }
}
