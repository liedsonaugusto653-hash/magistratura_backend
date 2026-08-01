package ao.magistratura.exception;

/**
 * Lançada quando o provider de IA (Ollama ou outro configurado) não
 * responde, recusa a ligação ou devolve um erro irrecuperável.
 */
public class IAIndisponivelException extends RuntimeException {

    public IAIndisponivelException(String message) {
        super(message);
    }

    public IAIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
