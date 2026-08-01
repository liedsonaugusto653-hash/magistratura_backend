package ao.magistratura.knowledge.retrieval;

import ao.magistratura.entity.Artigo;
import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.rag.LegalRetrievalService;
import ao.magistratura.rag.RetrievalResult;
import ao.magistratura.rag.RetrievedPassage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapta o {@link LegalRetrievalService} existente para a API da Knowledge Layer.
 * Mantém a lógica lexical/determinística já validada; não duplica queries.
 */
@Component
@RequiredArgsConstructor
public class LexicalLegalSearch implements LexicalSearch {

    private final LegalRetrievalService legalRetrievalService;

    @Override
    public List<KnowledgePassage> search(KnowledgeQuery query) {
        RetrievalResult rr = legalRetrievalService.recuperar(
                query.texto(), query.diplomaId(), query.artigoId());
        if (rr == null || rr.vazio()) {
            return List.of();
        }
        List<KnowledgePassage> out = new ArrayList<>();
        if (rr.passagens() != null && !rr.passagens().isEmpty()) {
            for (RetrievedPassage p : rr.passagens()) {
                out.add(new KnowledgePassage(
                        p.artigoId(),
                        p.artigoId(),
                        p.diplomaId(),
                        null,
                        KnowledgeContentKind.LEGISLACAO,
                        p.diplomaTitulo(),
                        null,
                        p.artigoNumero(),
                        p.artigoTitulo(),
                        null,
                        null,
                        p.texto(),
                        p.metodo(),
                        p.confianca()));
            }
            return out;
        }
        if (rr.artigos() != null) {
            for (Artigo a : rr.artigos()) {
                out.add(fromArtigo(a, rr.estrategia() != null ? rr.estrategia() : "LEXICAL", 0.6));
            }
        }
        return out;
    }

    private static KnowledgePassage fromArtigo(Artigo a, String metodo, double score) {
        return new KnowledgePassage(
                a.getId(),
                a.getId(),
                a.getDiploma() != null ? a.getDiploma().getId() : null,
                a.getDocumento() != null ? a.getDocumento().getId() : null,
                KnowledgeContentKind.LEGISLACAO,
                a.getDiploma() != null ? a.getDiploma().getTitulo() : null,
                a.getDiploma() != null ? a.getDiploma().getNumero() : null,
                a.getNumero(),
                a.getTitulo(),
                a.getCapitulo(),
                a.getSeccao(),
                a.getTexto(),
                metodo,
                score);
    }
}
