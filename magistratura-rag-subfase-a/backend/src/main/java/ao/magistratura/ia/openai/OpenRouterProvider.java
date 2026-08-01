package ao.magistratura.ia.openai;

import ao.magistratura.exception.IAIndisponivelException;
import ao.magistratura.ia.IaQuotaState;
import ao.magistratura.ia.AIProvider;
import ao.magistratura.ia.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * OpenRouter = API OpenAI-compatible. Reutiliza a mesma implementação
 * com propriedades app.openrouter.* e app.ia.provider=openrouter.
 */
@Component
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "openrouter")
public class OpenRouterProvider implements AIProvider {

    private final OpenAiCompatibleProvider delegate;

    public OpenRouterProvider(
            @Value("${app.openrouter.api-key:${app.openai.api-key:}}") String apiKey,
            @Value("${app.openrouter.model:openrouter/auto}") String model,
            @Value("${app.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${app.openrouter.timeout-seconds:120}") long timeoutSeconds,
            @Value("${app.openrouter.temperature:0.25}") double temperature,
            @Value("${app.openrouter.max-tokens:2048}") int maxTokens,
            IaQuotaState quotaState
    ) {
        // Instância dedicada (não é bean Spring) para evitar conflito de ConditionalOnProperty
        this.delegate = new OpenAiCompatibleProvider(apiKey, model, baseUrl, timeoutSeconds, temperature, maxTokens, quotaState);
    }

    @Override public String nome() { return "OpenRouter → " + delegate.nome(); }
    @Override public boolean disponivel() { return delegate.disponivel(); }
    @Override public String chat(List<ChatMessage> mensagens) { return delegate.chat(mensagens); }
    @Override public void chatStream(List<ChatMessage> mensagens, Consumer<String> onToken, Consumer<String> onComplete, Consumer<Throwable> onErro) {
        delegate.chatStream(mensagens, onToken, onComplete, onErro);
    }
}
