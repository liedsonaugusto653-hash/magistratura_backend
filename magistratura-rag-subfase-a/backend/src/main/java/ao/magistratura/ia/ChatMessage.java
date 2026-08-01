package ao.magistratura.ia;

/**
 * Mensagem genérica de uma conversa de IA.
 * <p>
 * Não depende de nenhum provider concreto — é o contrato que
 * {@link AIProvider} usa como entrada e saída, permitindo trocar o Ollama
 * por OpenAI, Gemini ou outro fornecedor sem alterar o resto do sistema.
 */
public record ChatMessage(ChatRole role, String content) {

    public static ChatMessage system(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }
}
