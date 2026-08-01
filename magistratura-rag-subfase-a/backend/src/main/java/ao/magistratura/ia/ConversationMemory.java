package ao.magistratura.ia;

import ao.magistratura.entity.Artigo;
import ao.magistratura.entity.AutorMensagem;
import ao.magistratura.entity.ConversaIa;
import ao.magistratura.entity.Diploma;
import ao.magistratura.entity.MensagemIa;
import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.repository.ConversaIaRepository;
import ao.magistratura.repository.MensagemIaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Histórico de conversas do Tutor IA.
 * <p>
 * Responsável exclusivamente por ler e persistir {@link ConversaIa} e
 * {@link MensagemIa}, e por converter esse histórico em {@link ChatMessage}
 * — o formato que {@link AIProvider} entende. Nenhuma outra classe deve
 * aceder diretamente aos repositórios de conversas/mensagens de IA.
 */
@Component
@RequiredArgsConstructor
public class ConversationMemory {

    /** Número máximo de mensagens anteriores enviadas como contexto ao modelo. */
    /** Últimas mensagens enviadas ao modelo (menos = menos latência em IA local). */
    private static final int JANELA_CONTEXTO = 8;

    private final ConversaIaRepository conversaRepository;
    private final MensagemIaRepository mensagemRepository;

    @Transactional
    public ConversaIa obterOuCriar(UUID conversaId, Utilizador utilizador) {
        if (conversaId == null) {
            return criarConversa(utilizador, "Nova conversa");
        }
        return obterDoUtilizador(conversaId, utilizador.getId());
    }

    @Transactional
    public ConversaIa criarConversa(Utilizador utilizador, String titulo) {
        ConversaIa conversa = ConversaIa.builder()
                .utilizador(utilizador)
                .titulo((titulo == null || titulo.isBlank()) ? "Nova conversa" : titulo.trim())
                .build();
        return conversaRepository.save(conversa);
    }

    @Transactional(readOnly = true)
    public ConversaIa obterDoUtilizador(UUID conversaId, UUID utilizadorId) {
        ConversaIa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada"));
        if (!conversa.getUtilizador().getId().equals(utilizadorId)) {
            throw new RecursoNaoEncontradoException("Conversa não encontrada");
        }
        return conversa;
    }

    @Transactional(readOnly = true)
    public List<ConversaIa> listarDoUtilizador(UUID utilizadorId) {
        return conversaRepository.findByUtilizadorIdOrderByDataCriacaoDesc(utilizadorId);
    }

    @Transactional
    public void eliminar(UUID conversaId, UUID utilizadorId) {
        ConversaIa conversa = obterDoUtilizador(conversaId, utilizadorId);
        conversaRepository.delete(conversa);
    }

    /**
     * Devolve o histórico da conversa já convertido para {@link ChatMessage},
     * limitado à janela de contexto — pronto para enviar ao {@link AIProvider}.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> historicoParaProvider(UUID conversaId) {
        List<MensagemIa> mensagens = mensagemRepository.findByConversaIdOrderByTimestampDesc(
                conversaId, PageRequest.of(0, JANELA_CONTEXTO));

        List<ChatMessage> resultado = new ArrayList<>(mensagens.size());
        mensagens.stream()
                .sorted(Comparator.comparing(MensagemIa::getTimestamp))
                .forEach(m -> resultado.add(
                        m.getAutor() == AutorMensagem.UTILIZADOR
                                ? ChatMessage.user(m.getConteudo())
                                : ChatMessage.assistant(m.getConteudo())));
        return resultado;
    }

    @Transactional
    public MensagemIa registarMensagemUtilizador(ConversaIa conversa, String conteudo,
                                                   Diploma diplomaContexto, Artigo artigoContexto) {
        MensagemIa mensagem = MensagemIa.builder()
                .conversa(conversa)
                .autor(AutorMensagem.UTILIZADOR)
                .conteudo(conteudo)
                .diplomaContexto(diplomaContexto)
                .artigoContexto(artigoContexto)
                .timestamp(Instant.now())
                .build();
        atualizarTituloSeNecessario(conversa, conteudo);
        return mensagemRepository.save(mensagem);
    }

    @Transactional
    public MensagemIa registarMensagemIA(ConversaIa conversa, String conteudo,
                                          Diploma diplomaContexto, Artigo artigoContexto) {
        return registarMensagemIA(conversa, conteudo, diplomaContexto, artigoContexto, null);
    }

    /**
     * Regista resposta da IA, opcionalmente com as fontes RAG serializadas em JSON
     * (marcadores [1], [2], …) para reabrir citações interactivas ao recarregar a conversa.
     */
    @Transactional
    public MensagemIa registarMensagemIA(ConversaIa conversa, String conteudo,
                                          Diploma diplomaContexto, Artigo artigoContexto,
                                          String fontesJson) {
        MensagemIa mensagem = MensagemIa.builder()
                .conversa(conversa)
                .autor(AutorMensagem.IA)
                .conteudo(conteudo)
                .diplomaContexto(diplomaContexto)
                .artigoContexto(artigoContexto)
                .fontesJson(normalizarFontesJson(fontesJson))
                .timestamp(Instant.now())
                .build();
        return mensagemRepository.save(mensagem);
    }

    @Transactional(readOnly = true)
    public List<MensagemIa> mensagensDaConversa(UUID conversaId) {
        return mensagemRepository.findByConversaIdOrderByTimestampAsc(conversaId);
    }

    /** Usa a primeira pergunta do estudante como título automático da conversa. */
    private void atualizarTituloSeNecessario(ConversaIa conversa, String primeiraMensagem) {
        if (!"Nova conversa".equals(conversa.getTitulo())) {
            return;
        }
        String titulo = primeiraMensagem.length() > 60
                ? primeiraMensagem.substring(0, 60).trim() + "…"
                : primeiraMensagem.trim();
        conversa.setTitulo(titulo);
        conversaRepository.save(conversa);
    }

    private static String normalizarFontesJson(String fontesJson) {
        if (fontesJson == null || fontesJson.isBlank() || "[]".equals(fontesJson.trim())) {
            return null;
        }
        return fontesJson.trim();
    }
}
