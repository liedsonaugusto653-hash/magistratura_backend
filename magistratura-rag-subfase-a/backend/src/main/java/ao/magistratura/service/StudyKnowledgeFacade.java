package ao.magistratura.service;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.Diploma;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.ia.PromptBuilder.ContextoJuridico;
import ao.magistratura.knowledge.api.KnowledgeContentKind;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.api.KnowledgeService;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DiplomaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fachada única dos módulos de estudo (Flashcards, Questões, Simulados)
 * para obter contexto jurídico via Knowledge Layer.
 * <p>
 * Fallback determinístico: se a Knowledge Layer não devolver passagens
 * (vector-store=noop ou diploma recém-processado sem embeddings),
 * carrega artigos reais da BD — o mesmo conteúdo que a Biblioteca mostra.
 */
@Component
@RequiredArgsConstructor
public class StudyKnowledgeFacade {

    private static final Logger log = LoggerFactory.getLogger(StudyKnowledgeFacade.class);
    private static final int FALLBACK_ARTIGOS = 8;

    private final KnowledgeService knowledgeService;
    private final DiplomaRepository diplomaRepository;
    private final ArtigoRepository artigoRepository;

    public ResolvedStudyContext resolve(UUID diplomaId, UUID artigoId) {
        KnowledgePassage pass = null;
        if (artigoId != null) {
            pass = knowledgeService.findArticle(artigoId).orElse(null);
            if (pass == null) {
                Artigo a = artigoRepository.findById(artigoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));
                pass = passagemDeArtigo(a, "BD_FALLBACK", 1.0);
            }
        }

        Diploma diploma = null;
        UUID effectiveDiplomaId = diplomaId;
        if (pass != null && pass.diplomaId() != null) {
            effectiveDiplomaId = pass.diplomaId();
        }
        if (effectiveDiplomaId != null) {
            diploma = diplomaRepository.findById(effectiveDiplomaId).orElse(null);
        }

        if (pass == null && diplomaId != null) {
            KnowledgeResult kr = knowledgeService.search(new KnowledgeQuery(
                    diploma != null && diploma.getTitulo() != null ? diploma.getTitulo() : "legislação",
                    diplomaId, null, null, KnowledgeContentKind.LEGISLACAO, FALLBACK_ARTIGOS, true));
            if (kr != null && !kr.vazio()) {
                return new ResolvedStudyContext(diploma, kr.passagens(),
                        ContextoJuridico.comPassagens(diploma, null, null, kr.passagens()));
            }
            List<KnowledgePassage> doDiploma = artigosDoDiplomaComoPassagens(diplomaId);
            if (!doDiploma.isEmpty()) {
                log.info("Knowledge Layer vazia para diploma {} — a usar {} artigos da BD",
                        diplomaId, doDiploma.size());
                return new ResolvedStudyContext(diploma, doDiploma,
                        ContextoJuridico.comPassagens(diploma, null, null, doDiploma));
            }
            if (diploma != null) {
                throw new RegraNegocioException(
                        "O diploma «" + diploma.getTitulo()
                                + "» ainda não tem artigos processados. "
                                + "Importa um PDF na Biblioteca, associa-o a este diploma e processa-o "
                                + "antes de gerar flashcards, questões ou simulados.");
            }
            throw new RecursoNaoEncontradoException("Diploma não encontrado");
        }

        if (pass == null && diploma == null) {
            throw new RegraNegocioException("É necessário indicar um diploma ou um artigo de origem");
        }

        List<KnowledgePassage> passagens = pass != null ? List.of(pass) : List.of();
        if (passagens.isEmpty() && diplomaId != null) {
            passagens = artigosDoDiplomaComoPassagens(diplomaId);
        }
        if (passagens.isEmpty()) {
            throw new RegraNegocioException(
                    "Não foi encontrado texto jurídico para o contexto seleccionado. "
                            + "Confirma que o diploma/artigo tem conteúdo processado na Biblioteca.");
        }
        return new ResolvedStudyContext(diploma, passagens,
                ContextoJuridico.comPassagens(diploma, null, null, passagens));
    }

    public KnowledgeResult search(String termo, UUID diplomaId, int limite) {
        int lim = limite > 0 ? limite : 5;
        KnowledgeResult kr = knowledgeService.search(new KnowledgeQuery(
                termo, diplomaId, null, null, KnowledgeContentKind.LEGISLACAO, lim, true));
        if (kr != null && !kr.vazio()) {
            return kr;
        }
        if (diplomaId != null) {
            List<KnowledgePassage> fallback = artigosDoDiplomaComoPassagens(diplomaId);
            if (!fallback.isEmpty()) {
                List<KnowledgePassage> limited = fallback.stream().limit(lim).toList();
                return new KnowledgeResult(limited, "BD_FALLBACK", limited.size(), false);
            }
        }
        if (termo != null && !termo.isBlank()) {
            List<Artigo> encontrados = artigoRepository
                    .pesquisar(termo.trim(), PageRequest.of(0, lim))
                    .getContent();
            if (!encontrados.isEmpty()) {
                List<KnowledgePassage> passagens = new ArrayList<>();
                for (Artigo a : encontrados) {
                    passagens.add(passagemDeArtigo(a, "BD_TEXTO", 0.7));
                }
                return new KnowledgeResult(passagens, "BD_TEXTO", passagens.size(), false);
            }
        }
        return kr != null ? kr : KnowledgeResult.vazioResultado();
    }

    private List<KnowledgePassage> artigosDoDiplomaComoPassagens(UUID diplomaId) {
        List<Artigo> artigos = artigoRepository.findByDiplomaIdOrderByOrdemAsc(diplomaId);
        if (artigos == null || artigos.isEmpty()) {
            return List.of();
        }
        List<KnowledgePassage> out = new ArrayList<>();
        int n = 0;
        for (Artigo a : artigos) {
            if (a.getTexto() == null || a.getTexto().isBlank()) {
                continue;
            }
            out.add(passagemDeArtigo(a, "BD_DIPLOMA", 0.85));
            if (++n >= FALLBACK_ARTIGOS) {
                break;
            }
        }
        return out;
    }

    private static KnowledgePassage passagemDeArtigo(Artigo a, String metodo, double score) {
        Diploma d = a.getDiploma();
        return new KnowledgePassage(
                a.getId(),
                a.getId(),
                d != null ? d.getId() : null,
                a.getDocumento() != null ? a.getDocumento().getId() : null,
                KnowledgeContentKind.LEGISLACAO,
                d != null ? d.getTitulo() : null,
                d != null ? d.getNumero() : null,
                a.getNumero(),
                a.getTitulo(),
                a.getCapitulo(),
                a.getSeccao(),
                a.getTexto(),
                metodo,
                score);
    }

    public record ResolvedStudyContext(
            Diploma diploma,
            List<KnowledgePassage> passagens,
            ContextoJuridico contextoJuridico
    ) {
        public UUID diplomaId() {
            return diploma != null ? diploma.getId() : null;
        }

        public UUID artigoId() {
            if (passagens == null || passagens.isEmpty()) {
                return null;
            }
            return passagens.get(0).artigoId();
        }
    }
}
