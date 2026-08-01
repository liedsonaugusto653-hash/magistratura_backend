package ao.magistratura.pipeline.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener síncrono de diagnóstico. Substituível por publicação assíncrona
 * ({@code @Async}, AMQP, etc.) sem alterar quem publica os eventos.
 */
@Component
public class PipelineEventLogger {

    private static final Logger log = LoggerFactory.getLogger(PipelineEventLogger.class);

    @EventListener
    public void onRecebido(PipelineEvents.DocumentoRecebido e) {
        log.info("[pipeline] DocumentoRecebido id={} hash={}", e.documentoId(), e.hash());
    }

    @EventListener
    public void onEtapa(PipelineEvents.EtapaConcluida e) {
        log.info("[pipeline] EtapaConcluida id={} etapa={} ok={} detalhe={}",
                e.documentoId(), e.etapa(), e.sucesso(), e.detalhe());
    }

    @EventListener
    public void onConcluido(PipelineEvents.PipelineConcluido e) {
        log.info("[pipeline] PipelineConcluido id={} artigos={}", e.documentoId(), e.artigosPersistidos());
    }

    @EventListener
    public void onFalhou(PipelineEvents.PipelineFalhou e) {
        log.warn("[pipeline] PipelineFalhou id={} etapa={} erro={}", e.documentoId(), e.etapa(), e.erro());
    }
}
