package ao.magistratura.service;

import ao.magistratura.dto.ia.GerarQuestoesRequest;
import ao.magistratura.dto.ia.GerarQuestoesResponse;
import ao.magistratura.dto.ia.QuestaoGeradaResponse;
import ao.magistratura.dto.questao.QuestaoCompletaResponse;
import ao.magistratura.dto.questao.QuestaoRequest;
import ao.magistratura.dto.questao.QuestaoResponse;
import ao.magistratura.dto.questao.ResponderQuestaoRequest;
import ao.magistratura.dto.questao.ResponderQuestaoResponse;
import ao.magistratura.entity.*;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.IaJsonParser;
import ao.magistratura.util.GeneratedStudyValidator;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.PromptBuilder;
import ao.magistratura.repository.DiplomaRepository;
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
    private final DiplomaRepository diplomaRepository;
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
    @Transactional(readOnly = true)
    public QuestaoCompletaResponse obterCompleta(UUID id) {
        Questao q = questaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada"));
        return new QuestaoCompletaResponse(
                q.getId(),
                q.getEnunciado(),
                q.getOpcaoA(),
                q.getOpcaoB(),
                q.getOpcaoC(),
                q.getOpcaoD(),
                q.getRespostaCorreta() != null ? q.getRespostaCorreta().name() : null,
                q.getJustificacao(),
                q.getNivelDificuldade() != null ? q.getNivelDificuldade().name() : null,
                q.getDiploma() != null ? q.getDiploma().getId() : null,
                q.getDiploma() != null ? q.getDiploma().getTitulo() : null
        );
    }

    @Transactional
    public QuestaoResponse criar(QuestaoRequest request) {
        Questao q = montarQuestao(new Questao(), request);
        q.setGenerationStatus("MANUAL");
        q = questaoRepository.save(q);
        return mapResponse(q);
    }

    @Transactional
    public QuestaoResponse actualizar(UUID id, QuestaoRequest request) {
        Questao q = questaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada"));
        montarQuestao(q, request);
        q = questaoRepository.save(q);
        return mapResponse(q);
    }

    private Questao montarQuestao(Questao q, QuestaoRequest request) {
        q.setEnunciado(request.enunciado().trim());
        q.setOpcaoA(request.opcaoA().trim());
        q.setOpcaoB(request.opcaoB().trim());
        q.setOpcaoC(request.opcaoC().trim());
        q.setOpcaoD(request.opcaoD().trim());
        q.setRespostaCorreta(OpcaoResposta.valueOf(request.respostaCorreta().trim().toUpperCase()));
        q.setJustificacao(request.justificacao() != null ? request.justificacao().trim() : null);
        if (request.nivelDificuldade() != null && !request.nivelDificuldade().isBlank()) {
            try {
                q.setNivelDificuldade(NivelDificuldade.valueOf(request.nivelDificuldade().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                q.setNivelDificuldade(NivelDificuldade.MEDIO);
            }
        } else if (q.getNivelDificuldade() == null) {
            q.setNivelDificuldade(NivelDificuldade.MEDIO);
        }
        if (request.diplomaId() != null) {
            q.setDiploma(diplomaRepository.findById(request.diplomaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado")));
        }
        return q;
    }

    @Transactional
    public void eliminar(UUID id) {
        Questao q = questaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada"));
        questaoRepository.delete(q);
    }

@Transactional
    public GerarQuestoesResponse gerarViaIa(GerarQuestoesRequest request) {
        ResolvedStudyContext ctx = studyKnowledgeFacade.resolve(request.diplomaId(), request.artigoId());
        int qtd = request.quantidadeOuDefeito();
        List<QuestaoGeradaResponse> geradas = gerarQuestoesComRetry(ctx, qtd);
        String ctxTexto = "";
        if (ctx.passagens() != null) {
            StringBuilder sb = new StringBuilder();
            for (var p : ctx.passagens()) {
                if (p != null && p.texto() != null) {
                    sb.append(p.texto()).append('\n');
                }
            }
            ctxTexto = sb.toString();
        }
        geradas = GeneratedStudyValidator.filtrarQuestoesValidas(geradas, ctxTexto);
        if (geradas.isEmpty()) {
            throw new ao.magistratura.exception.RegraNegocioException(
                    "A IA não produziu questões válidas e ancoradas no texto legal. "
                            + "Confirma que o diploma/artigo tem conteúdo processado e tenta de novo com menos itens.");
        }

        if (request.guardar()) {
            List<QuestaoGeradaResponse> persistidas = new ArrayList<>();
            Artigo artigoRef = null;
            if (ctx.artigoId() != null) {
                artigoRef = entityManager.getReference(Artigo.class, ctx.artigoId());
            }
            for (QuestaoGeradaResponse gerada : geradas) {
                OpcaoResposta correta = normalizarOpcao(gerada.respostaCorreta());
                if (correta == null) {
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


    private static OpcaoResposta normalizarOpcao(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase();
        if (!s.isEmpty()) {
            char c = s.charAt(0);
            if (c >= 'A' && c <= 'D') {
                try { return OpcaoResposta.valueOf(String.valueOf(c)); } catch (Exception ignored) {}
            }
        }
        try {
            String only = s.replaceAll("[^A-D]", "");
            if (only.length() == 1) return OpcaoResposta.valueOf(only);
        } catch (Exception ignored) {}
        return null;
    }

    
    private List<QuestaoGeradaResponse> gerarQuestoesComRetry(ResolvedStudyContext ctx, int qtd) {
        Exception ultimo = null;
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            int n = tentativa == 0 ? qtd : Math.min(qtd, 1);
            try {
                String prompt = promptBuilder.promptQuestoes(ctx.contextoJuridico(), n);
                String respostaJson = aiProvider.chatJson(List.of(
                        ChatMessage.system(prompt),
                        ChatMessage.user(
                                "Gera " + n + " questão(ões). Responde APENAS com o JSON completo e fechado, sem texto extra.")));
                List<QuestaoGeradaResponse> lista = extrairQuestoes(respostaJson);
                if (lista != null && !lista.isEmpty()) {
                    return lista;
                }
            } catch (Exception e) {
                ultimo = e;
                log.warn("Geração de questões tentativa {} falhou: {}", tentativa + 1, e.getMessage());
            }
        }
        if (ultimo instanceof RuntimeException re) {
            throw re;
        }
        throw new IAIndisponivelException(
                "A IA não devolveu questões num formato utilizável. Tenta 1 questão de cada vez.", ultimo);
    }

    private List<QuestaoGeradaResponse> extrairQuestoes(String respostaIA) {
        try {
            JsonNode raiz = IaJsonParser.garantirArray(IaJsonParser.parse(objectMapper, respostaIA), "questoes");
            List<QuestaoGeradaResponse> resultado = new ArrayList<>();
            for (JsonNode item : raiz.path("questoes")) {
                String enunciado = item.path("enunciado").asText("").trim();
                if (enunciado.isEmpty()) continue;
                resultado.add(new QuestaoGeradaResponse(
                        null,
                        enunciado,
                        item.path("opcaoA").asText(""),
                        item.path("opcaoB").asText(""),
                        item.path("opcaoC").asText(""),
                        item.path("opcaoD").asText(""),
                        item.path("respostaCorreta").asText(""),
                        item.path("justificacao").asText("")));
            }
            if (resultado.isEmpty()) {
                throw new IAIndisponivelException("A IA não devolveu questões num formato reconhecível");
            }
            return resultado;
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao interpretar questões geradas pela IA", e);
            throw new IAIndisponivelException(
                    "A IA devolveu uma resposta que não foi possível interpretar como questões: " + e.getMessage());
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
