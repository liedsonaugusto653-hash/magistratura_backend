package ao.magistratura.pipeline.event;

import ao.magistratura.pipeline.model.PipelineEtapa;

import java.util.UUID;

/**
 * Eventos de domínio do pipeline. Publicados via {@code ApplicationEventPublisher}.
 * No futuro, um listener pode reencaminhá-los para RabbitMQ/Kafka sem alterar
 * o orquestrador nem as etapas.
 */
public final class PipelineEvents {

    private PipelineEvents() {
    }

    public record DocumentoRecebido(UUID documentoId, String hash) {}

    public record EtapaConcluida(UUID documentoId, PipelineEtapa etapa, boolean sucesso, String detalhe) {}

    public record PipelineConcluido(UUID documentoId, int artigosPersistidos) {}

    public record PipelineFalhou(UUID documentoId, PipelineEtapa etapa, String erro) {}
}
