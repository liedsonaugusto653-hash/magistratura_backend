package ao.magistratura.service;

import ao.magistratura.dto.ia.ChatRequest;
import ao.magistratura.dto.ia.CitacaoFonteResponse;
import ao.magistratura.dto.ia.ConversaDetailResponse;
import ao.magistratura.dto.ia.ConversaSummaryResponse;
import ao.magistratura.dto.ia.ExplicarArtigoRequest;
import ao.magistratura.dto.ia.ExplicarArtigoResponse;
import ao.magistratura.dto.ia.FlashcardGeradoResponse;
import ao.magistratura.dto.ia.GerarFlashcardsRequest;
import ao.magistratura.dto.ia.GerarFlashcardsResponse;
import ao.magistratura.dto.ia.GerarQuestoesRequest;
import ao.magistratura.dto.ia.GerarQuestoesResponse;
import ao.magistratura.dto.ia.MensagemResponse;
import ao.magistratura.dto.ia.QuestaoGeradaResponse;
import ao.magistratura.dto.ia.ResumoIARequest;
import ao.magistratura.dto.ia.ResumoIAResponse;
import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.ConversaIa;
import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.Flashcard;
import ao.magistratura.entity.MensagemIa;
import ao.magistratura.entity.NivelDificuldade;
import ao.magistratura.entity.OpcaoResposta;
import ao.magistratura.entity.Questao;
import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.exception.RegraNegocioException;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import ao.magistratura.ia.ConversationMemory;
import ao.magistratura.ia.PromptBuilder;
import ao.magistratura.ia.PromptBuilder.ContextoJuridico;
import ao.magistratura.ia.ContextAssemblyService;
import ao.magistratura.ia.StreamingService;
import ao.magistratura.knowledge.api.KnowledgePassage;
import ao.magistratura.knowledge.api.KnowledgeQuery;
import ao.magistratura.knowledge.api.KnowledgeResult;
import ao.magistratura.knowledge.api.KnowledgeService;
import ao.magistratura.knowledge.api.StudyContextPolicy;
import ao.magistratura.repository.ArtigoRepository;
import ao.magistratura.repository.DiplomaRepository;
import ao.magistratura.repository.UtilizadorRepository;
import ao.magistratura.util.ResponseTextCleaner;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorService {

    private static final Logger log = LoggerFactory.getLogger(TutorService.class);

    private final AIProvider aiProvider;
    private final KnowledgeService knowledgeService;
    private final FlashcardService flashcardService;
    private final QuestaoService questaoService;
    private final PromptBuilder promptBuilder;
    private final ConversationMemory conversationMemory;
    private final StreamingService streamingService;
    private final ContextAssemblyService contextAssemblyService;

    private final UtilizadorRepository utilizadorRepository;
    private final DiplomaRepository diplomaRepository;
    private final ArtigoRepository artigoRepository;



    // ------------------------------------------------------------------
    // Conversas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ConversaSummaryResponse> listarConversas(String email) {
        Utilizador utilizador = utilizadorPorEmail(email);
        return conversationMemory.listarDoUtilizador(utilizador.getId()).stream()
                .map(this::paraResumo)
                .toList();
    }

    @Transactional
    public ConversaSummaryResponse criarConversa(String email, String titulo) {
        Utilizador utilizador = utilizadorPorEmail(email);
        ConversaIa conversa = conversationMemory.criarConversa(utilizador, titulo);
        return paraResumo(conversa);
    }

    @Transactional(readOnly = true)
    public ConversaDetailResponse obterConversa(String email, UUID conversaId) {
        Utilizador utilizador = utilizadorPorEmail(email);
        ConversaIa conversa = conversationMemory.obterDoUtilizador(conversaId, utilizador.getId());
        List<MensagemIa> mensagens = conversationMemory.mensagensDaConversa(conversaId);
        return new ConversaDetailResponse(
                conversa.getId(),
                conversa.getTitulo(),
                conversa.getDataCriacao(),
                conversa.getDataAtualizacao(),
                mensagens.stream().map(this::paraMensagemResponse).toList()
        );
    }

    @Transactional
    public void eliminarConversa(String email, UUID conversaId) {
        Utilizador utilizador = utilizadorPorEmail(email);
        conversationMemory.eliminar(conversaId, utilizador.getId());
    }

    // ------------------------------------------------------------------
    // Chat (síncrono)
    // ------------------------------------------------------------------

    @Transactional
    public MensagemResponse perguntar(String email, ChatRequest request) {
        Utilizador utilizador = utilizadorPorEmail(email);
        ConversaIa conversa = conversationMemory.obterOuCriar(request.conversaId(), utilizador);

        ContextoJuridico contexto = resolverContextoComRetrieval(request);
        Diploma diploma = contexto.diploma();
        Artigo artigo = contexto.artigo();

        conversationMemory.registarMensagemUtilizador(conversa, request.mensagem(), diploma, artigo);

        List<ChatMessage> mensagens = montarMensagensChat(conversa.getId(), contexto);

        List<CitacaoFonteResponse> fontes = mapearFontes(contexto);

        String resposta;
        try {
            resposta = ResponseTextCleaner.removerParagrafosDuplicados(aiProvider.chat(mensagens));
        } catch (IAIndisponivelException e) {
            log.error("Falha ao contactar o Tutor IA", e);
            throw e;
        }

        String fontesJson = serializarFontes(fontes);
        MensagemIa mensagemIa = conversationMemory.registarMensagemIA(
                conversa, resposta, diploma, artigo, fontesJson);
        return paraMensagemResponse(mensagemIa, fontes);
    }

    // ------------------------------------------------------------------
    // Chat (streaming via SSE)
    // ------------------------------------------------------------------

    @Transactional
    public SseEmitter perguntarStream(String email, ChatRequest request) {
        Utilizador utilizador = utilizadorPorEmail(email);
        ConversaIa conversa = conversationMemory.obterOuCriar(request.conversaId(), utilizador);

        ContextoJuridico contexto = resolverContextoComRetrieval(request);
        Diploma diploma = contexto.diploma();
        Artigo artigo = contexto.artigo();

        conversationMemory.registarMensagemUtilizador(conversa, request.mensagem(), diploma, artigo);

        List<ChatMessage> mensagens = montarMensagensChat(conversa.getId(), contexto);

        UUID conversaId = conversa.getId();
        List<CitacaoFonteResponse> fontes = mapearFontes(contexto);
        String fontesJson = serializarFontes(fontes);

        return streamingService.iniciar(mensagens, textoCompleto -> {
            // Executado na virtual thread do streaming, ao terminar a resposta.
            ConversaIa conversaAtual = conversationMemory.obterDoUtilizador(conversaId, utilizador.getId());
            conversationMemory.registarMensagemIA(
                    conversaAtual, textoCompleto, diploma, artigo, fontesJson);
        }, fontesJson, conversaId);
    }

    // ------------------------------------------------------------------
    // Geração: resumo
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResumoIAResponse resumir(ResumoIARequest request) {
        ContextoJuridico contexto = contextoDeResumo(request);
        if (contexto.isVazio()) {
            throw new RegraNegocioException("É necessário indicar um diploma, um artigo ou um texto para resumir");
        }

        String prompt = promptBuilder.promptResumo(contexto);
        String resposta = aiProvider.chat(List.of(ChatMessage.system(prompt),
                ChatMessage.user("Gera o resumo conforme as instruções.")));

        return new ResumoIAResponse(resposta.trim());
    }

    // ------------------------------------------------------------------
    // Geração: explicação de artigo
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ExplicarArtigoResponse explicarArtigo(ExplicarArtigoRequest request) {
        KnowledgePassage pass = knowledgeService.findArticle(request.artigoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));

        ContextoJuridico contexto = ContextoJuridico.comPassagens(
                null, null, request.trecho(), List.of(pass));
        String promptSistema = promptBuilder.promptSistemaTutor(contexto);

        String num = pass.artigoNumero() != null ? pass.artigoNumero() : "?";
        String pergunta = (request.trecho() != null && !request.trecho().isBlank())
                ? "Explica o seguinte trecho do artigo " + num + " de forma clara e didática."
                : "Explica o artigo " + num + " de forma clara e didática, com um exemplo prático se fizer sentido.";

        String resposta = aiProvider.chat(List.of(
                ChatMessage.system(promptSistema),
                ChatMessage.user(pergunta)
        ));

        return new ExplicarArtigoResponse(resposta.trim());
    }

    // ------------------------------------------------------------------
    // Geração: flashcards
    // ------------------------------------------------------------------

    @Transactional
    public GerarFlashcardsResponse gerarFlashcards(GerarFlashcardsRequest request) {
        return flashcardService.gerarViaIa(request);
    }

    // ------------------------------------------------------------------
    // Geração: questões
    // ------------------------------------------------------------------

    @Transactional
    public GerarQuestoesResponse gerarQuestoes(GerarQuestoesRequest request) {
        return questaoService.gerarViaIa(request);
    }

    // ------------------------------------------------------------------
    // Diagnóstico
    // ------------------------------------------------------------------

    public boolean iaDisponivel() {
        return aiProvider.disponivel();
    }

    public String nomeProvider() {
        return aiProvider.nome();
    }

    // ------------------------------------------------------------------
    // Auxiliares privados
    // ------------------------------------------------------------------

    private List<ChatMessage> montarMensagensChat(UUID conversaId, ContextoJuridico contexto) {
        String promptSistema = promptBuilder.promptSistemaTutor(contexto);
        List<ChatMessage> historico = conversationMemory.historicoParaProvider(conversaId);

        List<ChatMessage> mensagens = new ArrayList<>();
        mensagens.add(ChatMessage.system(promptSistema));
        mensagens.addAll(historico);
        return mensagens;
    }

    private ContextoJuridico contextoDeResumo(ResumoIARequest request) {
        Diploma diploma = diplomaOpcional(request.diplomaId());
        Artigo artigo = artigoOpcional(request.artigoId());
        return new ContextoJuridico(diploma, artigo, null, request.texto());
    }



    /**
     * Extensão RAG (Subfase A): combina IDs explícitos do pedido com recuperação
     * automática a partir do texto da pergunta, sem substituir o fluxo existente.
     */
    /**
     * RAG via KnowledgeService apenas — sem re-hidratar artigos por repository no path de chat.
     * IDs explícitos do pedido ainda resolvem Diploma/Artigo para metadados de mensagem.
     */
    private ContextoJuridico resolverContextoComRetrieval(ChatRequest request) {
        return contextAssemblyService.assembleForChat(request);
    }

    private Diploma diplomaOpcional(UUID id) {
        if (id == null) {
            return null;
        }
        return diplomaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diploma não encontrado"));
    }

    private Artigo artigoOpcional(UUID id) {
        if (id == null) {
            return null;
        }
        return artigoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));
    }

    private Utilizador utilizadorPorEmail(String email) {
        return utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));
    }

    private ConversaSummaryResponse paraResumo(ConversaIa conversa) {
        return new ConversaSummaryResponse(
                conversa.getId(), conversa.getTitulo(), conversa.getDataCriacao(), conversa.getDataAtualizacao());
    }

    private MensagemResponse paraMensagemResponse(MensagemIa mensagem) {
        return paraMensagemResponse(mensagem, desserializarFontes(mensagem.getFontesJson()));
    }

    private MensagemResponse paraMensagemResponse(MensagemIa mensagem, List<CitacaoFonteResponse> fontes) {
        return new MensagemResponse(
                mensagem.getId(),
                mensagem.getConversa().getId(),
                mensagem.getAutor().name(),
                mensagem.getConteudo(),
                mensagem.getDiplomaContexto() != null ? mensagem.getDiplomaContexto().getId() : null,
                mensagem.getArtigoContexto() != null ? mensagem.getArtigoContexto().getId() : null,
                mensagem.getTimestamp(),
                fontes != null ? fontes : List.of()
        );
    }

    private List<CitacaoFonteResponse> desserializarFontes(String fontesJson) {
        if (fontesJson == null || fontesJson.isBlank()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    fontesJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<CitacaoFonteResponse>>() {});
        } catch (Exception e) {
            log.warn("Falha ao desserializar fontes RAG da mensagem: {}", e.getMessage());
            return List.of();
        }
    }

    private static final int MAX_EXTRATO_FONTE = 480;

    /**
     * Converte as passagens do contexto em fontes numeradas [1]…[n] para o frontend.
     * A ordem deve coincidir com a numeração injectada no prompt pelo {@link PromptBuilder}.
     */
    private List<CitacaoFonteResponse> mapearFontes(ContextoJuridico contexto) {
        return contextAssemblyService.toFontes(contexto);
    }

    private String serializarFontes(List<CitacaoFonteResponse> fontes) {
        if (fontes == null || fontes.isEmpty()) {
            return "[]";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(fontes);
        } catch (Exception e) {
            log.warn("Falha ao serializar fontes RAG: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * Extrai o bloco JSON da resposta da IA, mesmo que o modelo tenha
     * acrescentado texto extra antes/depois apesar da instrução do prompt.
     */


    /** Isola o primeiro bloco {...} da resposta, caso o modelo tenha acrescentado texto fora do JSON. */



}