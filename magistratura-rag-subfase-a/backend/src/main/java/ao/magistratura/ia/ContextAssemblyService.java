package ao.magistratura.ia;

import ao.magistratura.dto.ia.ChatRequest;
import ao.magistratura.dto.ia.CitacaoFonteResponse;
import ao.magistratura.entity.Diploma;
import ao.magistratura.ia.PromptBuilder.ContextoJuridico;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.api.KnowledgeService;
import ao.magistratura.knowledge.api.StructuredContextComposer;
import ao.magistratura.knowledge.api.StudyContextPolicy;
import ao.magistratura.repository.DiplomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta o contexto jurídico MÍNIMO para o LLM.
 * <p>
 * Fluxo: pergunta → KnowledgeService (pesquisa + ranking) → política de truncagem → ContextoJuridico.
 * A IA nunca pesquisa; só recebe o que esta camada decide enviar.
 */
@Service
@RequiredArgsConstructor
public class ContextAssemblyService {

    private static final int MAX_EXTRATO_FONTE = 480;

    private final KnowledgeService knowledgeService;
    private final DiplomaRepository diplomaRepository;
    private final StructuredContextComposer structuredContextComposer;

    public ContextoJuridico assembleForChat(ChatRequest request) {
        KnowledgeResult kr = knowledgeService.search(KnowledgeQuery.juridicoComTopico(
                request.mensagem(),
                request.diplomaId(),
                request.artigoId(),
                request.topicoId(),
                StudyContextPolicy.CHAT_TOP_K
        ));

        List<KnowledgePassage> passagens = (kr != null && !kr.vazio())
                ? new ArrayList<>(kr.passagens())
                : new ArrayList<>();

        if (passagens.isEmpty() && request.artigoId() != null) {
            knowledgeService.findArticle(request.artigoId()).ifPresent(passagens::add);
        }

        // Diversificar papéis jurídicos antes de truncar
        passagens = structuredContextComposer.compose(
                passagens, StructuredContextComposer.Budget.chatDefault());
        passagens = StudyContextPolicy.limitar(passagens, StudyContextPolicy.CHAT_MAX_PASSAGENS_NO_PROMPT);
        passagens = StudyContextPolicy.truncarTextos(passagens, StudyContextPolicy.MAX_CHARS_POR_PASSAGEM);

        Diploma diploma = null;
        if (request.diplomaId() != null) {
            diploma = diplomaRepository.findById(request.diplomaId()).orElse(null);
        }

        return ContextoJuridico.comPassagens(diploma, null, request.trecho(), passagens);
    }

    public List<CitacaoFonteResponse> toFontes(ContextoJuridico contexto) {
        if (contexto == null || contexto.passagensRecuperadas() == null
                || contexto.passagensRecuperadas().isEmpty()) {
            return List.of();
        }
        var limitadas = StudyContextPolicy.limitar(
                contexto.passagensRecuperadas(), StudyContextPolicy.CHAT_MAX_PASSAGENS_NO_PROMPT);
        List<CitacaoFonteResponse> out = new ArrayList<>();
        int n = 1;
        for (KnowledgePassage p : limitadas) {
            String extrato = p.texto() != null ? p.texto() : "";
            if (extrato.length() > MAX_EXTRATO_FONTE) {
                extrato = extrato.substring(0, MAX_EXTRATO_FONTE) + "…";
            }
            out.add(new CitacaoFonteResponse(
                    n,
                    p.artigoId(),
                    p.diplomaId(),
                    p.diplomaTitulo(),
                    p.diplomaNumero(),
                    p.artigoNumero(),
                    p.artigoTitulo(),
                    p.capitulo(),
                    p.seccao(),
                    extrato,
                    p.metodo(),
                    p.score()
            ));
            n++;
        }
        return out;
    }
}
