package ao.magistratura.ia;

import java.util.List;
import java.util.function.Consumer;

/**
 * Abstração do fornecedor de Inteligência Artificial.
 * <p>
 * Nenhuma outra camada do sistema (Controllers, Services) deve conhecer
 * detalhes do Ollama. Toda a comunicação passa por esta interface, para que
 * seja possível substituir o provider por OpenAI, Gemini ou outro, criando
 * apenas uma nova implementação — sem alterar {@code TutorService} nem
 * {@code TutorController}.
 */
public interface AIProvider {

    /**
     * Nome do provider (usado em logs e para diagnóstico via /api/ia/status).
     */
    String nome();

    /**
     * Verifica se o provider está acessível e pronto a responder.
     */
    boolean disponivel();

    /**
     * Envia uma conversa completa e devolve a resposta integral, de uma vez.
     * Usado quando não é necessário streaming (ex.: geração de flashcards,
     * questões ou resumos, onde se espera um bloco de texto/JSON completo).
     *
     * @throws IAIndisponivelException se o provider não responder ou falhar
     */
    String chat(List<ChatMessage> mensagens);

    /**
     * Como {@link #chat}, mas pede resposta em JSON (quando o provider suportar).
     * Implementação por omissão: delega em {@link #chat}.
     */
    default String chatJson(List<ChatMessage> mensagens) {
        return chat(mensagens);
    }

    /**
     * Envia uma conversa completa e transmite a resposta token a token,
     * à medida que é gerada pelo modelo.
     *
     * @param mensagens  histórico da conversa, terminando na pergunta atual
     * @param onToken    invocado para cada fragmento de texto gerado
     * @param onComplete invocado uma única vez quando a resposta termina, com o texto completo
     * @param onErro     invocado se ocorrer uma falha durante o streaming
     */
    void chatStream(List<ChatMessage> mensagens,
                     Consumer<String> onToken,
                     Consumer<String> onComplete,
                     Consumer<Throwable> onErro);
}
