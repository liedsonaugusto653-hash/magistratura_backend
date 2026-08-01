package ao.magistratura.service;

import ao.magistratura.dto.flashcard.FlashcardRequest;
import ao.magistratura.dto.flashcard.FlashcardResponse;
import ao.magistratura.dto.flashcard.FlashcardResponse.ProgressoResumo;
import ao.magistratura.dto.flashcard.FlashcardRevisarRequest;
import ao.magistratura.dto.flashcard.FlashcardRevisarResponse;
import ao.magistratura.dto.ia.FlashcardGeradoResponse;
import ao.magistratura.dto.ia.GerarFlashcardsRequest;
import ao.magistratura.dto.ia.GerarFlashcardsResponse;
import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.Flashcard;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.IaJsonParser;
import ao.magistratura.util.GeneratedStudyValidator;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.PromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ao.magistratura.entity.FlashcardProgresso;
import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.repository.FlashcardProgressoRepository;
import ao.magistratura.repository.DiplomaRepository;
import ao.magistratura.repository.FlashcardRepository;
import ao.magistratura.repository.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardRepository flashcardRepository;
    private final DiplomaRepository diplomaRepository;
    private final FlashcardProgressoRepository progressoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final StudyKnowledgeFacade studyKnowledgeFacade;
    private final AIProvider aiProvider;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<FlashcardResponse> listar(String email) {
        Utilizador utilizador = obterUtilizador(email);

        Map<UUID, FlashcardProgresso> progressoMap = progressoRepository
                .findByUtilizadorId(utilizador.getId())
                .stream()
                .collect(Collectors.toMap(p -> p.getFlashcard().getId(), p -> p));

        return flashcardRepository.findAll().stream()
                .map(f -> mapResponse(f, progressoMap.get(f.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardResponse obter(UUID id, String email) {
        Utilizador utilizador = obterUtilizador(email);
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Flashcard não encontrado"));

        FlashcardProgresso progresso = progressoRepository
                .findByUtilizadorIdAndFlashcardId(utilizador.getId(), id)
                .orElse(null);

        return mapResponse(flashcard, progresso);
    }

    @Transactional
    public FlashcardRevisarResponse revisar(UUID flashcardId, FlashcardRevisarRequest request, String email) {
        Utilizador utilizador = obterUtilizador(email);
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Flashcard não encontrado"));

        FlashcardProgresso progresso = progressoRepository
                .findByUtilizadorIdAndFlashcardId(utilizador.getId(), flashcardId)
                .orElseGet(() -> FlashcardProgresso.builder()
                        .utilizador(utilizador)
                        .flashcard(flashcard)
                        .build());

        boolean acertou = Boolean.TRUE.equals(request.acertou());
        progresso.setVezesRevisto(progresso.getVezesRevisto() + 1);
        if (acertou) {
            progresso.setAcertos(progresso.getAcertos() + 1);
        } else {
            progresso.setErros(progresso.getErros() + 1);
        }
        progresso.setUltimaRevisao(Instant.now());
        progresso.setNivelDificuldade(calcularNivel(progresso));
        progresso.setProximaRevisao(calcularProximaRevisao(progresso, acertou));

        progressoRepository.save(progresso);

        double percentagem = progresso.getVezesRevisto() == 0
                ? 0.0
                : (progresso.getAcertos() * 100.0) / progresso.getVezesRevisto();

        return new FlashcardRevisarResponse(
                flashcardId,
                acertou,
                progresso.getVezesRevisto(),
                progresso.getAcertos(),
                progresso.getErros(),
                Math.round(percentagem * 10.0) / 10.0,
                progresso.getNivelDificuldade()
        );
    }

    private String calcularNivel(FlashcardProgresso p) {
        if (p.getVezesRevisto() == 0) return "MEDIO";
        double taxa = (p.getAcertos() * 100.0) / p.getVezesRevisto();
        if (taxa >= 80) return "FACIL";
        if (taxa >= 50) return "MEDIO";
        return "DIFICIL";
    }

    private Instant calcularProximaRevisao(FlashcardProgresso p, boolean acertou) {
        // Espaçamento simples: acerto → +3 dias; erro → +1 dia
        int dias = acertou ? 3 : 1;
        if (p.getVezesRevisto() >= 5 && acertou) dias = 7;
        return Instant.now().plus(dias, ChronoUnit.DAYS);
    }

    private FlashcardResponse mapResponse(Flashcard f, FlashcardProgresso p) {
        ProgressoResumo progressoDto = null;
        if (p != null) {
            double percentagem = p.getVezesRevisto() == 0
                    ? 0.0
                    : (p.getAcertos() * 100.0) / p.getVezesRevisto();
            progressoDto = new ProgressoResumo(
                    p.getVezesRevisto(),
                    p.getAcertos(),
                    p.getErros(),
                    Math.round(percentagem * 10.0) / 10.0,
                    p.getNivelDificuldade()
            );
        }
        return new FlashcardResponse(
                f.getId(),
                f.getPergunta(),
                f.getResposta(),
                f.getTema() != null ? f.getTema().getId() : null,
                f.getTema() != null ? f.getTema().getNome() : null,
                f.getCategoria() != null ? f.getCategoria().getId() : null,
                f.getCategoria() != null ? f.getCategoria().getNome() : null,
                f.getDiploma() != null ? f.getDiploma().getId() : null,
                f.getDiploma() != null ? f.getDiploma().getTitulo() : null,
                progressoDto
        );
    }




    @Transactional
    public FlashcardResponse criar(FlashcardRequest request, String email) {
        Utilizador utilizador = obterUtilizador(email);
        Flashcard f = Flashcard.builder()
                .pergunta(request.pergunta().trim())
                .resposta(request.resposta().trim())
                .diploma(resolverDiploma(request.diplomaId()))
                .generationStatus("MANUAL")
                .build();
        f = flashcardRepository.save(f);
        return mapResponse(f, null);
    }

    @Transactional
    public FlashcardResponse actualizar(UUID id, FlashcardRequest request, String email) {
        obterUtilizador(email);
        Flashcard f = flashcardRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Flashcard não encontrado"));
        f.setPergunta(request.pergunta().trim());
        f.setResposta(request.resposta().trim());
        if (request.diplomaId() != null) {
            f.setDiploma(resolverDiploma(request.diplomaId()));
        }
        f = flashcardRepository.save(f);
        return mapResponse(f, null);
    }

    private Diploma resolverDiploma(UUID diplomaId) {
        if (diplomaId == null) {
            return null;
        }
        return diplomaRepository.findById(diplomaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado"));
    }

    @Transactional
    public void eliminar(UUID id) {
        Flashcard f = flashcardRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Flashcard não encontrado"));
        progressoRepository.deleteByFlashcardId(id);
        flashcardRepository.delete(f);
    }

    /**
     * Gera flashcards via IA com contexto jurídico obtido exclusivamente da Knowledge Layer.
     */
    @Transactional
    public GerarFlashcardsResponse gerarViaIa(GerarFlashcardsRequest request) {
        StudyKnowledgeFacade.ResolvedStudyContext ctx = studyKnowledgeFacade.resolve(request.diplomaId(), request.artigoId());
        int qtd = request.quantidadeOuDefeito();
        List<FlashcardGeradoResponse> gerados = gerarFlashcardsComRetry(ctx, qtd);

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
        gerados = GeneratedStudyValidator.filtrarFlashcardsValidos(gerados, ctxTexto);
        if (gerados.isEmpty()) {
            throw new RegraNegocioException(
                    "A IA não produziu flashcards válidos e ancorados no texto legal. "
                            + "Confirma que o diploma/artigo tem conteúdo processado e tenta de novo.");
        }

        if (request.guardar()) {
            List<FlashcardGeradoResponse> persistidos = new ArrayList<>();
            for (FlashcardGeradoResponse gerado : gerados) {
                Flashcard flashcard = Flashcard.builder()
                        .pergunta(gerado.pergunta())
                        .resposta(gerado.resposta())
                        .diploma(ctx.diploma())
                        .build();
                flashcard = flashcardRepository.save(flashcard);
                persistidos.add(new FlashcardGeradoResponse(
                        flashcard.getId(), flashcard.getPergunta(), flashcard.getResposta()));
            }
            return new GerarFlashcardsResponse(persistidos, true);
        }
        return new GerarFlashcardsResponse(gerados, false);
    }

    
    private List<FlashcardGeradoResponse> gerarFlashcardsComRetry(StudyKnowledgeFacade.ResolvedStudyContext ctx, int qtd) {
        Exception ultimo = null;
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            int n = tentativa == 0 ? qtd : Math.min(qtd, 2);
            try {
                String prompt = promptBuilder.promptFlashcards(ctx.contextoJuridico(), n);
                String respostaJson = aiProvider.chatJson(List.of(
                        ChatMessage.system(prompt),
                        ChatMessage.user(
                                "Gera " + n + " flashcard(s). Responde APENAS com o JSON completo e fechado, sem texto extra.")));
                List<FlashcardGeradoResponse> lista = extrairFlashcards(respostaJson);
                if (lista != null && !lista.isEmpty()) {
                    return lista;
                }
            } catch (Exception e) {
                ultimo = e;
                log.warn("Geração de flashcards tentativa {} falhou: {}", tentativa + 1, e.getMessage());
            }
        }
        if (ultimo instanceof RuntimeException re) {
            throw re;
        }
        throw new IAIndisponivelException(
                "A IA não devolveu flashcards num formato utilizável. Tenta 2 cards de cada vez.", ultimo);
    }

    private List<FlashcardGeradoResponse> extrairFlashcards(String respostaIA) {
        try {
            JsonNode raiz = IaJsonParser.garantirArray(IaJsonParser.parse(objectMapper, respostaIA), "flashcards");
            List<FlashcardGeradoResponse> resultado = new ArrayList<>();
            for (JsonNode item : raiz.path("flashcards")) {
                String pergunta = item.path("pergunta").asText(item.path("frente").asText("")).trim();
                String resposta = item.path("resposta").asText(item.path("verso").asText("")).trim();
                if (pergunta.isEmpty() || resposta.isEmpty()) continue;
                resultado.add(new FlashcardGeradoResponse(null, pergunta, resposta));
            }
            if (resultado.isEmpty()) {
                throw new IAIndisponivelException("A IA não devolveu flashcards num formato reconhecível");
            }
            return resultado;
        } catch (IAIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao interpretar flashcards gerados pela IA", e);
            throw new IAIndisponivelException(
                    "A IA devolveu uma resposta que não foi possível interpretar como flashcards: " + e.getMessage());
        }
    }

    private Utilizador obterUtilizador(String email) {
        return utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
    }
}
