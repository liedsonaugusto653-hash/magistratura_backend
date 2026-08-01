package ao.magistratura.service;

import ao.magistratura.dto.ia.GerarQuestoesRequest;
import ao.magistratura.dto.ia.GerarQuestoesResponse;
import ao.magistratura.dto.ia.QuestaoGeradaResponse;
import ao.magistratura.dto.questao.QuestaoResponse;
import ao.magistratura.dto.questao.ResponderQuestaoRequest;
import ao.magistratura.dto.questao.ResponderQuestaoResponse;
import ao.magistratura.entity.*;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.IaJsonExtractor;
import ao.magistratura.ia.PromptBuilder;
import ao.magistratura.repository.QuestaoRepository;
import ao.magistratura.repository.RespostaEstudanteRepository;
import ao.magistratura.repository.UtilizadorRepository;
import ao.magistratura.service.StudyKnowledgeFacade.ResolvedStudyContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestaoService {

    private static final Logger log = LoggerFactory.getLogger(QuestaoService.class);

    private final QuestaoRepository questaoRepository;
    private final RespostaEstudanteRepository respostaEstudanteRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final StudyKnowledgeFacade studyKnowledgeFacade;
    private final AIProvider aiProvider;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<QuestaoResponse> listar(Pageable pageable) {
        return questaoRepository.findAll(pageable).map(this::mapResponse);
    }

    @Transactional(readOnly = true)
    public QuestaoResponse obter(UUID id) {
        Questao q = questaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada"));
        return mapResponse(q);
    }

    @Transactional
    public ResponderQuestaoResponse responder(UUID questaoId, ResponderQuestaoRequest request, String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));

        Questao questao = questaoRepository.findById(questaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada"));

        OpcaoResposta escolhida = OpcaoResposta.valueOf(request.resposta().trim().toUpperCase());
        boolean correta = escolhida == questao.getRespostaCorreta();

        RespostaEstudante resposta = RespostaEstudante.builder()
                .utilizador(utilizador)
                .questao(questao)
                .respostaEscolhida(escolhida)
                .correta(correta)
                .build();
        respostaEstudanteRepository.save(resposta);

        return new ResponderQuestaoResponse(
                questaoId,
                escolhida.name(),
                questao.getRespostaCorreta().name(),
                correta,
                questao.getJustificacao()
        );
    }

    /**
     * Gera questões via IA com contexto jurídico obtido exclusivamente da Knowledge Layer.
     */
    @Transactional
    public GerarQuestoesResponse gerarViaIa(GerarQuestoesRequest request) {
        ResolvedStudyContext ctx = studyKnowledgeFacade.resolve(request.diplomaId(), request.artigoId());
        String prompt = promptBuilder.promptQuestoes(ctx.contextoJuridico(), request.quantidadeOuDefeito());
        String respostaJson = aiProvider.chat(List.of(
                ChatMessage.system(prompt),
                ChatMessage.user("Gera as questões conforme as instruções, apenas em JSON.")));

        List<QuestaoGeradaResponse> geradas = extrairQuestoes(respostaJson);

        if (request.guardar()) {
            List<QuestaoGeradaResponse> persistidas = new ArrayList<>();
            Artigo artigoRef = null;
            if (ctx.artigoId() != null) {
                artigoRef = entityManager.getReference(Artigo.class, ctx.artigoId());
            }
            for (QuestaoGeradaResponse gerada : geradas) {
                OpcaoResposta correta;
                try {
                    correta = OpcaoResposta.valueOf(gerada.respostaCorreta().trim().toUpperCase());
                } catch (Exception e) {
                    log.warn("Questão gerada pela IA com resposta correta inválida: {}", gerada.respostaCorreta());
                    continue;
                }
                Questao questao = Questao.builder()
                        .enunciado(gerada.enunciado())
                        .opcaoA(gerada.opcaoA())
                        .opcaoB(gerada.opcaoB())
                        .opcaoC(gerada.opcaoC())
                        .opcaoD(gerada.opcaoD())
                        .respostaCorreta(correta)
                        .justificacao(gerada.justificacao())
                        .diploma(ctx.diploma())
                        .artigo(artigoRef)
                        .nivelDificuldade(NivelDificuldade.MEDIO)
                        .build();
                questao = questaoRepository.save(questao);
                persistidas.add(new QuestaoGeradaResponse(
                        questao.getId(), questao.getEnunciado(), questao.getOpcaoA(), questao.getOpcaoB(),
                        questao.getOpcaoC(), questao.getOpcaoD(), questao.getRespostaCorreta().name(),
                        questao.getJustificacao()));
            }
            return new GerarQuestoesResponse(persistidas, true);
        }
        return new GerarQuestoesResponse(geradas, false);
    }


    private List<QuestaoGeradaResponse> extrairQuestoes(String respostaIA) {
        try {
            JsonNode raiz = IaJsonExtractor.parseObjectOrArray(respostaIA);
            var arr = IaJsonExtractor.arrayQuestoes(raiz);
            List<QuestaoGeradaResponse> resultado = new ArrayList<>();
            for (JsonNode item : arr) {
                String enunciado = IaJsonExtractor.texto(item, "enunciado", "question", "pergunta", "texto");
                if (enunciado.isBlank()) {
                    continue;
                }
                String opcaoA = IaJsonExtractor.texto(item, "opcaoA", "opcao_a", "A", "a");
                String opcaoB = IaJsonExtractor.texto(item, "opcaoB", "opcao_b", "B", "b");
                String opcaoC = IaJsonExtractor.texto(item, "opcaoC", "opcao_c", "C", "c");
                String opcaoD = IaJsonExtractor.texto(item, "opcaoD", "opcao_d", "D", "d");
                // alternativas aninhadas
                if (opcaoA.isBlank() && item.path("alternativas").isArray()) {
                    var alts = item.path("alternativas");
                    if (alts.size() > 0) opcaoA = alts.get(0).asText("");
                    if (alts.size() > 1) opcaoB = alts.get(1).asText("");
                    if (alts.size() > 2) opcaoC = alts.get(2).asText("");
                    if (alts.size() > 3) opcaoD = alts.get(3).asText("");
                }
                String correta = IaJsonExtractor.normalizarRespostaCorreta(
                        IaJsonExtractor.texto(item, "respostaCorreta", "resposta_correta", "correct", "correctAnswer", "gabarito"));
                String just = IaJsonExtractor.texto(item, "justificacao", "justification", "explicacao", "explanation");
                resultado.add(new QuestaoGeradaResponse(
                        null, enunciado, opcaoA, opcaoB, opcaoC, opcaoD, correta, just));
            }
            if (resultado.isEmpty()) {
                log.warn("Resposta IA (questões) sem itens reconhecíveis. Trecho: {}",
                        respostaIA != null && respostaIA.length() > 400 ? respostaIA.substring(0, 400) : respostaIA);
                throw new IAIndisponivelException("A IA não devolveu questões num formato reconhecível");
            }
            return resultado;
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao interpretar questões geradas pela IA. Trecho: {}",
                    respostaIA != null && respostaIA.length() > 500 ? respostaIA.substring(0, 500) : respostaIA, e);
            throw new IAIndisponivelException("A IA devolveu uma resposta que não foi possível interpretar como questões");
        }
    }

    private QuestaoResponse mapResponse(Questao q) {
        return new QuestaoResponse(
                q.getId(),
                q.getEnunciado(),
                q.getOpcaoA(),
                q.getOpcaoB(),
                q.getOpcaoC(),
                q.getOpcaoD(),
                q.getNivelDificuldade() != null ? q.getNivelDificuldade().name() : null,
                q.getTema() != null ? q.getTema().getId() : null,
                q.getTema() != null ? q.getTema().getNome() : null,
                q.getCategoria() != null ? q.getCategoria().getId() : null,
                q.getCategoria() != null ? q.getCategoria().getNome() : null,
                q.getDiploma() != null ? q.getDiploma().getId() : null,
                q.getDiploma() != null ? q.getDiploma().getTitulo() : null
        );
    }
}
