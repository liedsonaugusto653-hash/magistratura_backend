package ao.magistratura.service;

import ao.magistratura.entity.TopicoArtigo;
import ao.magistratura.entity.TopicoJuridico;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.PromptBuilder;
import ao.magistratura.ia.PromptBuilder.ContextoJuridico;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeService;
import ao.magistratura.repository.TopicoArtigoRepository;
import ao.magistratura.repository.TopicoJuridicoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Geração de fichas de estudo (IA) — extraído de {@link OntologiaService} (SRP).
 * Persistência e mapeamento de resposta ficam no serviço de orquestração.
 */
@Service
@RequiredArgsConstructor
public class OntologiaFichaService {

    private static final Logger log = LoggerFactory.getLogger(OntologiaFichaService.class);
    private static final int MAX_ARTIGOS_CONTEXTO_FICHA = 6;

    private final TopicoJuridicoRepository topicoRepo;
    private final TopicoArtigoRepository topicoArtigoRepo;
    private final KnowledgeService knowledgeService;
    private final PromptBuilder promptBuilder;
    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gera (ou devolve cache) a ficha pedagógica do tópico.
     * @return entidade persistida (já com campos de estudo preenchidos)
     */
    @Transactional
    public TopicoJuridico gerarOuObter(UUID topicoId, boolean forcar) {
        TopicoJuridico topico = topicoRepo.findById(topicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tópico jurídico não encontrado"));

        if (!forcar && topico.getDefinicaoEstudo() != null && !topico.getDefinicaoEstudo().isBlank()) {
            return topico;
        }

        List<TopicoArtigo> ligacoes = topicoArtigoRepo.findByTopicoIdComArtigo(topicoId);
        if (ligacoes.isEmpty()) {
            throw new RegraNegocioException(
                    "Este tópico ainda não tem artigos ligados. Liga pelo menos um artigo "
                            + "(por exemplo, sincronizando a biblioteca) antes de gerar a ficha de estudo.");
        }

        List<KnowledgePassage> passagens = new ArrayList<>();
        for (TopicoArtigo ligacao : ligacoes) {
            knowledgeService.findArticle(ligacao.getArtigo().getId()).ifPresent(passagens::add);
            if (passagens.size() >= MAX_ARTIGOS_CONTEXTO_FICHA) {
                break;
            }
        }
        if (passagens.isEmpty()) {
            throw new RegraNegocioException(
                    "Os artigos ligados a este tópico ainda não estão indexados. Tenta novamente após "
                            + "processar a biblioteca.");
        }

        ContextoJuridico contexto = ContextoJuridico.comPassagens(null, null, null, passagens);
        String prompt = promptBuilder.promptFichaEstudo(contexto, topico.getNome(), topico.getDescricao());

        String respostaJson;
        try {
            respostaJson = aiProvider.chatJson(List.of(
                    ChatMessage.system(prompt),
                    ChatMessage.user(
                            "Gera a ficha de estudo deste conceito. Responde APENAS com o JSON completo e fechado, sem texto extra.")));
        } catch (Exception e) {
            log.warn("Falha ao gerar ficha de estudo para o tópico {}: {}", topicoId, e.getMessage());
            throw new IAIndisponivelException(
                    "Não foi possível gerar a ficha de estudo agora. Tenta novamente daqui a pouco.", e);
        }

        aplicarJsonNaEntidade(topico, topicoId, respostaJson);
        return topicoRepo.save(topico);
    }

    private void aplicarJsonNaEntidade(TopicoJuridico topico, UUID topicoId, String respostaJson) {
        String definicao = "";
        String porqueExiste = "";
        List<String> ondeAparece = new ArrayList<>();
        List<String> errosComuns = new ArrayList<>();
        String casoPraticoJson = null;
        List<java.util.Map<String, String>> perguntas = new ArrayList<>();

        try {
            JsonNode raiz = objectMapper.readTree(respostaJson);
            // Aceitar tanto objecto raiz como texto com JSON embutido
            if (!raiz.isObject() && respostaJson != null) {
                int i = respostaJson.indexOf('{');
                int j = respostaJson.lastIndexOf('}');
                if (i >= 0 && j > i) {
                    raiz = objectMapper.readTree(respostaJson.substring(i, j + 1));
                }
            }
            definicao = text(raiz, "definicao", "definição", "definition");
            porqueExiste = text(raiz, "porqueExiste", "porquê", "porque_existe");
            ondeAparece = texts(raiz, "ondeApareceVida", "ondeAparece", "exemplos");
            errosComuns = texts(raiz, "errosComuns", "erros");
            if (raiz.has("casoPratico") && !raiz.get("casoPratico").isNull()) {
                casoPraticoJson = objectMapper.writeValueAsString(raiz.get("casoPratico"));
            }
            JsonNode pq = raiz.path("perguntasGuia");
            if (!pq.isArray()) pq = raiz.path("perguntas");
            if (pq.isArray()) {
                for (JsonNode item : pq) {
                    String pergunta = item.path("pergunta").asText("").trim();
                    if (pergunta.isEmpty()) pergunta = item.path("q").asText("").trim();
                    String resposta = item.path("resposta").asText("").trim();
                    if (resposta.isEmpty()) resposta = item.path("a").asText("").trim();
                    if (!pergunta.isEmpty()) {
                        perguntas.add(java.util.Map.of("pergunta", pergunta, "resposta", resposta));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Falha ao interpretar ficha de estudo gerada pela IA para o tópico {}", topicoId, e);
            throw new IAIndisponivelException(
                    "A IA devolveu uma resposta que não foi possível interpretar como ficha de estudo: "
                            + e.getMessage());
        }

        if (definicao.isBlank() || perguntas.isEmpty()) {
            throw new IAIndisponivelException(
                    "A IA não devolveu uma ficha de estudo válida para este conceito. Tenta novamente.");
        }

        try {
            topico.setDefinicaoEstudo(definicao);
            topico.setPerguntasGuia(objectMapper.writeValueAsString(perguntas));
            topico.setPerguntasGuiaGeradoEm(Instant.now());
            topico.setPorqueExiste(porqueExiste.isBlank() ? null : porqueExiste);
            topico.setOndeApareceVida(ondeAparece.isEmpty() ? null : objectMapper.writeValueAsString(ondeAparece));
            topico.setErrosComuns(errosComuns.isEmpty() ? null : objectMapper.writeValueAsString(errosComuns));
            topico.setCasoPratico(casoPraticoJson);
        } catch (Exception e) {
            throw new IAIndisponivelException("Falha ao guardar a ficha de estudo gerada.", e);
        }
    }

    private static String text(JsonNode raiz, String... keys) {
        for (String k : keys) {
            String v = raiz.path(k).asText("").trim();
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private static List<String> texts(JsonNode raiz, String... keys) {
        for (String k : keys) {
            JsonNode n = raiz.path(k);
            if (n.isArray()) {
                List<String> out = new ArrayList<>();
                for (JsonNode item : n) {
                    String s = item.isTextual() ? item.asText("").trim() : item.path("texto").asText("").trim();
                    if (!s.isEmpty()) out.add(s);
                }
                if (!out.isEmpty()) return out;
            }
        }
        return List.of();
    }
}
