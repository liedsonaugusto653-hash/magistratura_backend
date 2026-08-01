package ao.magistratura.ia;

/**
 * Papel de uma mensagem numa conversa de IA, no formato universal
 * (compatível com Ollama, OpenAI e Gemini), independente do provider usado.
 */
public enum ChatRole {
    SYSTEM,
    USER,
    ASSISTANT
}
