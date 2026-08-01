package ao.magistratura.service;

import ao.magistratura.pipeline.event.PipelineEvents;
import ao.magistratura.pipeline.model.PipelineEtapa;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Fan-out SSE do progresso de processamento de documentos.
 * O cliente abre GET /api/documentos/{id}/progress e recebe eventos
 * {@code progress} / {@code done} / {@code error}.
 */
@Service
public class DocumentoProgressHub {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProgressHub.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 min (OCR longo)
    private static final long HEARTBEAT_SECONDS = 15L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    /** Heartbeats por emitter — cancelados no cleanup. */
    private final ConcurrentHashMap<SseEmitter, ScheduledFuture<?>> heartbeats = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "doc-progress-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public DocumentoProgressHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(UUID documentoId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(documentoId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            ScheduledFuture<?> hb = heartbeats.remove(emitter);
            if (hb != null) {
                hb.cancel(false);
            }
            remove(documentoId, emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            log.debug("SSE progresso doc {}: timeout", documentoId);
            cleanup.run();
        });
        emitter.onError(e -> {
            log.debug("SSE progresso doc {}: cliente desligou — {}", documentoId,
                    e != null ? e.getMessage() : "");
            cleanup.run();
        });

        ScheduledFuture<?> hb = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{\"ts\":" + System.currentTimeMillis() + "}"));
            } catch (Exception e) {
                cleanup.run();
            }
        }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
        heartbeats.put(emitter, hb);

        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(json(Map.of(
                            "documentoId", documentoId.toString(),
                            "estado", "LIGADO",
                            "mensagem", "A aguardar actualizações…",
                            "percentagem", 0
                    ))));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    public void publishProgress(UUID documentoId, String estado, String mensagem, Integer percentagem, Integer artigos) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("documentoId", documentoId.toString());
        payload.put("estado", estado != null ? estado : "PROCESSANDO");
        if (mensagem != null) payload.put("mensagem", mensagem);
        if (percentagem != null) payload.put("percentagem", percentagem);
        if (artigos != null) payload.put("artigos", artigos);
        broadcast(documentoId, "progress", payload);
    }

    public void publishDone(UUID documentoId, String estado, int artigos, String mensagem) {
        broadcast(documentoId, "done", Map.of(
                "documentoId", documentoId.toString(),
                "estado", estado != null ? estado : "PROCESSADO",
                "artigos", artigos,
                "mensagem", mensagem != null ? mensagem : "Processamento concluído",
                "percentagem", 100
        ));
        completeAll(documentoId);
    }

    public void publishError(UUID documentoId, String mensagem) {
        publishError(documentoId, mensagem, "ERRO");
    }

    public void publishError(UUID documentoId, String mensagem, String estado) {
        String est = (estado != null && !estado.isBlank()) ? estado : "ERRO";
        broadcast(documentoId, "error", Map.of(
                "documentoId", documentoId.toString(),
                "estado", est,
                "mensagem", mensagem != null ? mensagem : "Falha no processamento",
                "percentagem", 0
        ));
        completeAll(documentoId);
    }

    @EventListener
    public void onEtapa(PipelineEvents.EtapaConcluida ev) {
        int pct = percentagemEtapa(ev.etapa());
        String msg = mensagemEtapaAmigavel(ev.etapa(), ev.sucesso(), ev.detalhe());
        publishProgress(ev.documentoId(),
                ev.sucesso() ? "PROCESSANDO" : "ERRO",
                msg, pct, null);
    }

    @EventListener
    public void onConcluido(PipelineEvents.PipelineConcluido ev) {
        publishDone(ev.documentoId(), "PROCESSADO", ev.artigosPersistidos(),
                "Processado com " + ev.artigosPersistidos() + " artigo(s)");
    }

    @EventListener
    public void onFalhou(PipelineEvents.PipelineFalhou ev) {
        String msg = ev.erro() != null ? ev.erro() : "Falha no processamento";
        String estado = DocumentoEstadoService.isFalhaExtracao(msg) ? "FALHA_EXTRACAO" : "ERRO";
        publishError(ev.documentoId(), msg, estado);
    }


    private static String mensagemEtapaAmigavel(PipelineEtapa etapa, boolean sucesso, String detalhe) {
        if (detalhe != null && detalhe.toUpperCase().contains("PROTECTED")) {
            return sucesso ? "PDF com protecção detectada…" : "PDF protegido — não é possível extrair texto";
        }
        String base = switch (etapa) {
            case RECEBIDO, DETECAO_INCREMENTAL, VALIDADO -> sucesso ? "A preparar o documento…" : "Falha na validação";
            case ANALISANDO_PDF -> sucesso ? "A verificar o PDF…" : "Não foi possível analisar o PDF";
            case EXTRAINDO_PDF -> sucesso ? "A extrair o texto…" : "Falha ao extrair o texto";
            case OCR_EM_EXECUCAO -> sucesso ? "A ler páginas (OCR)…" : "Falha no OCR";
            case NORMALIZANDO_TEXTO -> sucesso ? "A limpar o texto…" : "Falha na normalização";
            case EXTRAINDO_METADADOS -> sucesso ? "A ler metadados…" : "Falha nos metadados";
            case EXTRAINDO_ESTRUTURA -> sucesso ? "A identificar artigos…" : "Falha ao estruturar artigos";
            case PERSISTINDO_ARTIGOS -> sucesso ? "A guardar artigos…" : "Falha ao guardar artigos";
            case INDEXANDO -> sucesso ? "A indexar…" : "Falha na indexação";
            case LIGANDO_ONTOLOGIA -> sucesso ? "A ligar conceitos jurídicos…" : "Falha na ligação ontológica";
            case GERANDO_CONHECIMENTO -> sucesso ? "A preparar conhecimento…" : "Falha na camada de conhecimento";
            case CONCLUIDO -> "Concluído";
            case ERRO -> "Erro no processamento";
        };
        return base;
    }

    private int percentagemEtapa(PipelineEtapa etapa) {
        if (etapa == null) return 5;
        return switch (etapa) {
            case DETECAO_INCREMENTAL -> 10;
            case ANALISANDO_PDF -> 20;
            case EXTRAINDO_PDF -> 45;
            case EXTRAINDO_ESTRUTURA -> 60;
            case PERSISTINDO_ARTIGOS -> 75;
            case INDEXANDO -> 82;
            case LIGANDO_ONTOLOGIA -> 88;
            case GERANDO_CONHECIMENTO -> 90;
            case CONCLUIDO -> 100;
            default -> 30;
        };
    }

    private void broadcast(UUID documentoId, String eventName, Map<String, ?> payload) {
        List<SseEmitter> list = emitters.get(documentoId);
        if (list == null || list.isEmpty()) return;
        String data = json(payload);
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                remove(documentoId, emitter);
            }
        }
    }

    private void completeAll(UUID documentoId) {
        List<SseEmitter> list = emitters.remove(documentoId);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            ScheduledFuture<?> hb = heartbeats.remove(emitter);
            if (hb != null) {
                hb.cancel(false);
            }
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private void remove(UUID documentoId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(documentoId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(documentoId, list);
            }
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"erro\":\"serialização\"}";
        }
    }
}
