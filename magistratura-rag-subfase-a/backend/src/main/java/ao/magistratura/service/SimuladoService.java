package ao.magistratura.service;

import ao.magistratura.dto.ia.GerarQuestoesRequest;
import ao.magistratura.dto.ia.GerarQuestoesResponse;
import ao.magistratura.dto.ia.QuestaoGeradaResponse;
import ao.magistratura.dto.questao.QuestaoResponse;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.IaJsonParser;
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


    @Transactional
    public void eliminar(UUID id) {
        Simulado s = simuladoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado não encontrado"));
        simuladoRepository.delete(s);
    }

    /**
     * Gera um simulado completo: Knowledge Layer → IA → Questões → Simulado.
     */
    @Transactional
    public GerarSimuladoResponse gerarViaIa(GerarSimuladoRequest request) {
        throw new RegraNegocioException(
                "A geração de simulados por IA está desactivada. "
                        + "Usa Questões e Flashcards para praticar com a legislação da biblioteca.");
    }


    /** Aceita "A", "a", "A)", "opcaoA", "1", etc. Devolve null se não reconhecer. */
    private static OpcaoResposta normalizarOpcao(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        if (!s.isEmpty()) {
            char c = s.charAt(0);
            if (c >= 'A' && c <= 'D') {
                try {
                    return OpcaoResposta.valueOf(String.valueOf(c));
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        if (s.contains("OPCAOA") || s.equals("1") || s.contains("OPÇÃO A") || s.contains("OPCAO A")) {
            return OpcaoResposta.A;
        }
        if (s.contains("OPCAOB") || s.equals("2") || s.contains("OPÇÃO B") || s.contains("OPCAO B")) {
            return OpcaoResposta.B;
        }
        if (s.contains("OPCAOC") || s.equals("3") || s.contains("OPÇÃO C") || s.contains("OPCAO C")) {
            return OpcaoResposta.C;
        }
        if (s.contains("OPCAOD") || s.equals("4") || s.contains("OPÇÃO D") || s.contains("OPCAO D")) {
            return OpcaoResposta.D;
        }
        try {
            String only = s.replaceAll("[^A-D]", "");
            if (only.length() == 1) {
                return OpcaoResposta.valueOf(only);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private List<QuestaoGeradaResponse> extrairQuestoes(String respostaIA) {
        try {
            JsonNode raiz = IaJsonParser.garantirArray(IaJsonParser.parse(objectMapper, respostaIA), "questoes");
            List<QuestaoGeradaResponse> resultado = new ArrayList<>();
            for (JsonNode item : raiz.path("questoes")) {
                String enunciado = item.path("enunciado").asText("").trim();
                if (enunciado.isEmpty()) {
                    enunciado = item.path("pergunta").asText("").trim();
                }
                if (enunciado.isEmpty()) continue;
                String correta = item.path("respostaCorreta").asText(
                        item.path("correta").asText(
                                item.path("gabarito").asText(""))).trim();
                resultado.add(new QuestaoGeradaResponse(
                        null,
                        enunciado,
                        item.path("opcaoA").asText(item.path("a").asText("")),
                        item.path("opcaoB").asText(item.path("b").asText("")),
                        item.path("opcaoC").asText(item.path("c").asText("")),
                        item.path("opcaoD").asText(item.path("d").asText("")),
                        correta,
                        item.path("justificacao").asText(item.path("explicacao").asText(""))));
            }
            return resultado;
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao interpretar questões do simulado: {}", e.getMessage());
            throw new IAIndisponivelException(
                    "A resposta da IA não continha JSON de questões válido. "
                            + "Tenta de novo ou reduz o número de questões. Detalhe: " + e.getMessage());
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