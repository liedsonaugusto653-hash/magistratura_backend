package ao.magistratura.rag;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;

import java.util.UUID;

/**
 * Um artigo recuperado da biblioteca, com metadados de ranking.
 */
public record RetrievedPassage(
        UUID artigoId,
        UUID diplomaId,
        String diplomaTitulo,
        String artigoNumero,
        String artigoTitulo,
        String texto,
        String metodo,   // ID_EXPLICITO | NUMERO_DIPLOMA | NUMERO | TEXTO
        double confianca
) {
    public static RetrievedPassage fromArtigo(Artigo a, String metodo, double confianca) {
        Diploma d = a.getDiploma();
        return new RetrievedPassage(
                a.getId(),
                d != null ? d.getId() : null,
                d != null ? d.getTitulo() : null,
                a.getNumero(),
                a.getTitulo(),
                a.getTexto(),
                metodo,
                confianca
        );
    }
}
