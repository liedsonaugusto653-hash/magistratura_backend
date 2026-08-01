package ao.magistratura.service;

import ao.magistratura.dto.ia.GerarQuestoesRequest;
import ao.magistratura.dto.ia.GerarQuestoesResponse;
import ao.magistratura.dto.ia.QuestaoGeradaResponse;
import ao.magistratura.dto.questao.QuestaoResponse;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.ia.IaJsonExtractor;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.PromptBuilder;
import ao.magistratura.service.StudyKnowledgeFacade.ResolvedStudyContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ao.magistratura.dto.simulado.*;
import ao.magistratura.entity.*;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.repository.*;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.api.StudyContextPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimuladoService {

    private final SimuladoRepository simuladoRepository;
    private final TentativaSimuladoRepository tentativaRepository;
    private final RespostaSimuladoRepository respostaSimuladoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final StudyKnowledgeFacade studyKnowledgeFacade;
    private final QuestaoRepository questaoRepository;
    private final AIProvider aiProvider;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(SimuladoService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<SimuladoResumoResponse> listar() {
        return simuladoRepository.findAll().stream()
                .map(s -> new SimuladoResumoResponse(
                        s.getId(),
                        s.getTitulo(),
                        s.getDescricao(),
                        s.getTempoMinutos(),
                        s.getPerguntas() != null ? s.getPerguntas().size() : 0,
                        s.getCategoria() != null ? s.getCategoria().getId() : null,
                        s.getCategoria() != null ? s.getCategoria().getNome() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SimuladoDetailResponse obter(UUID id) {
        Simulado s = simuladoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado não encontrado"));
        List<QuestaoResponse> questoes = s.getPerguntas().stream()
                .sorted(Comparator.comparingInt(SimuladoQuestao::getOrdem))
                .map(sq -> mapQuestao(sq.getQuestao()))
                .toList();
        return new SimuladoDetailResponse(
                s.getId(), s.getTitulo(), s.getDescricao(), s.getTempoMinutos(),
                s.getCategoria() != null ? s.getCategoria().getId() : null,
                s.getCategoria() != null ? s.getCategoria().getNome() : null,
                questoes
        );
    }

    @Transactional
    public IniciarSimuladoResponse iniciar(UUID simuladoId, String email) {
        Utilizador utilizador = obterUtilizador(email);
        Simulado simulado = simuladoRepository.findById(simuladoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado não encontrado"));

        if (simulado.getPerguntas() == null || simulado.getPerguntas().isEmpty()) {
            throw new RegraNegocioException("Este simulado não tem questões configuradas");
        }

        TentativaSimulado tentativa = TentativaSimulado.builder()
                .utilizador(utilizador)
                .simulado(simulado)
                .dataInicio(Instant.now())
                .concluido(false)
                .build();
        tentativaRepository.save(tentativa);

        List<QuestaoResponse> questoes = simulado.getPerguntas().stream()
                .sorted(Comparator.comparingInt(SimuladoQuestao::getOrdem))
                .map(sq -> mapQuestao(sq.getQuestao()))
                .toList();

        return new IniciarSimuladoResponse(
                tentativa.getId(),
                simulado.getId(),
                simulado.getTitulo(),
                simulado.getTempoMinutos(),
                tentativa.getDataInicio(),
                questoes
        );
    }

    @Transactional
    public void responder(UUID tentativaId, ResponderSimuladoRequest request, String email) {
        TentativaSimulado tentativa = obterTentativaDoUtilizador(tentativaId, email);
        if (tentativa.isConcluido()) {
            throw new RegraNegocioException("Esta tentativa já foi finalizada");
        }

        Questao questao = tentativa.getSimulado().getPerguntas().stream()
                .map(SimuladoQuestao::getQuestao)
                .filter(q -> q.getId().equals(request.questaoId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não pertence a este simulado"));

        OpcaoResposta escolhida = OpcaoResposta.valueOf(request.resposta().trim().toUpperCase());
        boolean correta = escolhida == questao.getRespostaCorreta();

        RespostaSimulado resposta = respostaSimuladoRepository
                .findByTentativaIdAndQuestaoId(tentativaId, request.questaoId())
                .orElseGet(() -> RespostaSimulado.builder()
                        .tentativa(tentativa)
                        .questao(questao)
                        .build());

        resposta.setRespostaEscolhida(escolhida);
        resposta.setCorreta(correta);
        respostaSimuladoRepository.save(resposta);
    }

    @Transactional
    public FinalizarSimuladoResponse finalizar(UUID tentativaId, String email) {
        TentativaSimulado tentativa = obterTentativaDoUtilizador(tentativaId, email);
        if (tentativa.isConcluido()) {
            throw new RegraNegocioException("Esta tentativa já foi finalizada");
        }

        List<RespostaSimulado> respostas = respostaSimuladoRepository.findByTentativaId(tentativaId);
        int total = tentativa.getSimulado().getPerguntas().size();
        int acertos = (int) respostas.stream().filter(RespostaSimulado::isCorreta).count();
        int erros = total - acertos;
        double pontuacao = total == 0 ? 0.0 : (acertos * 100.0) / total;

        tentativa.setDataFim(Instant.now());
        tentativa.setPontuacao(Math.round(pontuacao * 10.0) / 10.0);
        tentativa.setConcluido(true);
        tentativaRepository.save(tentativa);

        return new FinalizarSimuladoResponse(
                tentativa.getId(),
                tentativa.getSimulado().getId(),
                tentativa.getDataInicio(),
                tentativa.getDataFim(),
                total,
                acertos,
                erros,
                tentativa.getPontuacao(),
                true
        );
    }

    @Transactional(readOnly = true)
    public List<FinalizarSimuladoResponse> historico(String email) {
        Utilizador utilizador = obterUtilizador(email);
        return tentativaRepository.findByUtilizadorIdOrderByDataInicioDesc(utilizador.getId()).stream()
                .filter(TentativaSimulado::isConcluido)
                .map(t -> {
                    int total = t.getSimulado().getPerguntas() != null ? t.getSimulado().getPerguntas().size() : 0;
                    int acertos = t.getPontuacao() != null && total > 0
                            ? (int) Math.round(t.getPontuacao() * total / 100.0) : 0;
                    return new FinalizarSimuladoResponse(
                            t.getId(), t.getSimulado().getId(),
                            t.getDataInicio(), t.getDataFim(),
                            total, acertos, total - acertos,
                            t.getPontuacao() != null ? t.getPontuacao() : 0.0,
                            t.isConcluido()
                    );
                })
                .toList();
    }


    /**
     * Gera um simulado completo: Knowledge Layer → IA → Questões → Simulado.
     */
    @Transactional
    public GerarSimuladoResponse gerarViaIa(GerarSimuladoRequest request) {
        ResolvedStudyContext ctx;
        if (request.artigoId() != null || request.diplomaId() != null) {
            ctx = studyKnowledgeFacade.resolve(request.diplomaId(), request.artigoId());
        } else if (request.assunto() != null && !request.assunto().isBlank()) {
            var kr = studyKnowledgeFacade.search(request.assunto(), null, StudyContextPolicy.SIMULADO_TOP_K);
            if (kr.vazio()) {
                throw new RegraNegocioException("Não foi encontrado conhecimento jurídico para o assunto indicado");
            }
            var limited = StudyContextPolicy.limitar(kr.passagens(), StudyContextPolicy.SIMULADO_MAX_PASSAGENS);
            ctx = new ResolvedStudyContext(null, limited,
                    ao.magistratura.ia.PromptBuilder.ContextoJuridico.comPassagens(
                            null, null, request.assunto(), limited));
        } else {
            throw new RegraNegocioException("Indique diplomaId, artigoId ou assunto");
        }

        String prompt = promptBuilder.promptQuestoes(ctx.contextoJuridico(), request.quantidadeOuDefeito());
        if (request.assuntoOuVazio().length() > 0) {
            prompt = prompt + "\n\nFoco do simulado: " + request.assuntoOuVazio();
        }
        if (request.dificuldade() != null && !request.dificuldade().isBlank()) {
            prompt = prompt + "\nNível de dificuldade pretendido: " + request.dificuldade().trim();
        }

        String respostaJson = aiProvider.chat(List.of(
                ChatMessage.system(prompt),
                ChatMessage.user("Gera as questões do simulado em JSON, conforme as instruções.")));

        List<QuestaoGeradaResponse> geradas = extrairQuestoes(respostaJson);
        if (geradas.isEmpty()) {
            throw new IAIndisponivelException("A IA não gerou questões utilizáveis para o simulado");
        }

        NivelDificuldade nivel = NivelDificuldade.MEDIO;
        if (request.dificuldade() != null) {
            try {
                nivel = NivelDificuldade.valueOf(request.dificuldade().trim().toUpperCase());
            } catch (Exception ignored) {
                nivel = NivelDificuldade.MEDIO;
            }
        }

        Artigo artigoRef = null;
        if (ctx.artigoId() != null) {
            artigoRef = entityManager.getReference(Artigo.class, ctx.artigoId());
        }

        List<Questao> salvas = new ArrayList<>();
        for (QuestaoGeradaResponse g : geradas) {
            OpcaoResposta correta;
            try {
                correta = OpcaoResposta.valueOf(g.respostaCorreta().trim().toUpperCase());
            } catch (Exception e) {
                log.warn("Questão de simulado com resposta inválida: {}", g.respostaCorreta());
                continue;
            }
            Questao q = Questao.builder()
                    .enunciado(g.enunciado())
                    .opcaoA(g.opcaoA())
                    .opcaoB(g.opcaoB())
                    .opcaoC(g.opcaoC())
                    .opcaoD(g.opcaoD())
                    .respostaCorreta(correta)
                    .justificacao(g.justificacao())
                    .diploma(ctx.diploma())
                    .artigo(artigoRef)
                    .nivelDificuldade(nivel)
                    .build();
            salvas.add(questaoRepository.save(q));
        }
        if (salvas.isEmpty()) {
            throw new IAIndisponivelException("Nenhuma questão válida foi persistida");
        }
        int pedidas = request.quantidadeOuDefeito();
        if (salvas.size() < Math.max(1, pedidas / 2)) {
            throw new IAIndisponivelException(
                    "A IA devolveu demasiadas poucas questões válidas (" + salvas.size()
                            + " de " + pedidas + " pedidas). Tente novamente.");
        }
        if (salvas.size() < pedidas) {
            log.warn("Simulado gerado com {} questões (pedido {}); a continuar com o conjunto parcial",
                    salvas.size(), pedidas);
        }

        Simulado simulado = Simulado.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .tempoMinutos(request.tempoOuDefeito())
                .build();

        int ordem = 1;
        for (Questao q : salvas) {
            SimuladoQuestao sq = SimuladoQuestao.builder()
                    .simulado(simulado)
                    .questao(q)
                    .ordem(ordem++)
                    .build();
            simulado.getPerguntas().add(sq);
        }
        simulado = simuladoRepository.save(simulado);

        String origem = ctx.passagens() != null && !ctx.passagens().isEmpty()
                ? "KNOWLEDGE_LAYER (" + ctx.passagens().size() + " passagens)"
                : "KNOWLEDGE_LAYER";

        return new GerarSimuladoResponse(
                simulado.getId(),
                simulado.getTitulo(),
                salvas.size(),
                simulado.getTempoMinutos(),
                origem);
    }


    private List<QuestaoGeradaResponse> extrairQuestoes(String respostaIA) {
        try {
            JsonNode raiz = IaJsonExtractor.parseObjectOrArray(respostaIA);
            var arr = IaJsonExtractor.arrayQuestoes(raiz);
            List<QuestaoGeradaResponse> resultado = new ArrayList<>();
            for (JsonNode item : arr) {
                String enunciado = IaJsonExtractor.texto(item, "enunciado", "question", "pergunta", "texto");
                if (enunciado.isBlank()) continue;
                String opcaoA = IaJsonExtractor.texto(item, "opcaoA", "opcao_a", "A", "a");
                String opcaoB = IaJsonExtractor.texto(item, "opcaoB", "opcao_b", "B", "b");
                String opcaoC = IaJsonExtractor.texto(item, "opcaoC", "opcao_c", "C", "c");
                String opcaoD = IaJsonExtractor.texto(item, "opcaoD", "opcao_d", "D", "d");
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
            return resultado;
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao interpretar questões do simulado. Trecho: {}",
                    respostaIA != null && respostaIA.length() > 500 ? respostaIA.substring(0, 500) : respostaIA, e);
            throw new IAIndisponivelException("Resposta da IA inválida para simulado");
        }
    }

    /**
     * Obtém contexto jurídico da Knowledge Layer para um tema/diploma de simulado.
     * Não altera a lógica de tentativas; serve de suporte a geração/explicações futuras.
     */
    @Transactional(readOnly = true)
    public KnowledgeResult contextoJuridico(String termo, UUID diplomaId, int limite) {
        return studyKnowledgeFacade.search(termo, diplomaId, limite > 0 ? limite : 5);
    }

    private TentativaSimulado obterTentativaDoUtilizador(UUID tentativaId, String email) {
        Utilizador utilizador = obterUtilizador(email);
        TentativaSimulado tentativa = tentativaRepository.findById(tentativaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tentativa não encontrada"));
        if (!tentativa.getUtilizador().getId().equals(utilizador.getId())) {
            throw new RegraNegocioException("Esta tentativa não pertence ao utilizador autenticado");
        }
        return tentativa;
    }

    private Utilizador obterUtilizador(String email) {
        return utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
    }

    private QuestaoResponse mapQuestao(Questao q) {
        return new QuestaoResponse(
                q.getId(), q.getEnunciado(),
                q.getOpcaoA(), q.getOpcaoB(), q.getOpcaoC(), q.getOpcaoD(),
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