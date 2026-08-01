package ao.magistratura.ia;

import ao.magistratura.util.ResponseTextCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Liga o streaming token-a-token do {@link AIProvider} a um {@link SseEmitter}.
 * <p>
 * Resiliência:
 * <ul>
 *   <li>Heartbeat a cada {@value #HEARTBEAT_SECONDS}s (evita timeouts de proxy)</li>
 *   <li>Cancelamento imediato quando o cliente fecha (onCompletion / onTimeout / onError)</li>
 *   <li>Interrompe a virtual thread do Ollama para libertar recursos</li>
 *   <li>Não persiste resposta parcial se o cliente desligou a meio</li>
 *   <li>Evento opcional {@code fontes} antes dos tokens (citações RAG)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StreamingService {

    private static final Logger log = LoggerFactory.getLogger(StreamingService.class);
    private static final long TIMEOUT_MS = 10 * 60 * 1000L;
    private static final long HEARTBEAT_SECONDS = 15L;

    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Scheduler partilhado para heartbeats (daemon). */
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public SseEmitter iniciar(List<ChatMessage> mensagens, Consumer<String> aoConcluir) {
        return iniciar(mensagens, aoConcluir, null, null);
    }

    public SseEmitter iniciar(List<ChatMessage> mensagens, Consumer<String> aoConcluir, String fontesJson) {
        return iniciar(mensagens, aoConcluir, fontesJson, null);
    }

    /**
     * @param fontesJson JSON array de fontes RAG, ou null/blank se não houver
     * @param conversaId id da conversa já criada — emitido no início para o cliente
     *                   evitar fallback síncrono a criar uma segunda conversa
     */
    public SseEmitter iniciar(List<ChatMessage> mensagens, Consumer<String> aoConcluir,
                              String fontesJson, java.util.UUID conversaId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        AtomicBoolean clienteLigado = new AtomicBoolean(true);
        AtomicBoolean respostaPersistida = new AtomicBoolean(false);
        AtomicReference<Thread> workerRef = new AtomicReference<>();

        Runnable desligar = () -> {
            if (clienteLigado.compareAndSet(true, false)) {
                Thread w = workerRef.get();
                if (w != null && w.isAlive()) {
                    w.interrupt();
                    log.info("SSE Tutor: cliente desligado — a interromper worker de streaming");
                }
            }
        };

        emitter.onCompletion(desligar);
        emitter.onTimeout(() -> {
            log.warn("SSE Tutor: timeout");
            desligar.run();
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        emitter.onError(ex -> {
            log.debug("SSE Tutor: onError — {}", ex != null ? ex.getMessage() : "null");
            desligar.run();
        });

        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                () -> {
                    if (!clienteLigado.get()) {
                        return;
                    }
                    try {
                        emitter.send(SseEmitter.event().name("heartbeat").data("{\"ts\":" + System.currentTimeMillis() + "}"));
                    } catch (Exception e) {
                        // Cliente morto / proxy fechou — cancela o stream
                        desligar.run();
                    }
                },
                HEARTBEAT_SECONDS,
                HEARTBEAT_SECONDS,
                TimeUnit.SECONDS
        );

        Thread worker = Thread.ofVirtual().name("tutor-ia-stream-", 0).start(() -> {
            try {
                // Primeiro evento: id da conversa (evita conversas duplicadas no fallback do frontend)
                if (conversaId != null && clienteLigado.get()) {
                    enviarEvento(emitter, clienteLigado, "conversa",
                            "{\"id\":\"" + conversaId + "\"}");
                }
                if (fontesJson != null && !fontesJson.isBlank() && clienteLigado.get()) {
                    enviarEvento(emitter, clienteLigado, "fontes", fontesJson);
                }

                aiProvider.chatStream(
                        mensagens,
                        token -> {
                            if (!clienteLigado.get() || Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            enviarEvento(emitter, clienteLigado, "token", codificarJson(token));
                        },
                        textoCompleto -> {
                            if (!clienteLigado.get()) {
                                log.info("SSE Tutor: resposta ignorada (cliente já desligado)");
                                return;
                            }
                            concluir(emitter, clienteLigado, respostaPersistida, aoConcluir, textoCompleto);
                        },
                        erro -> {
                            if (!clienteLigado.get()) {
                                log.debug("SSE Tutor: erro ignorado após desligar — {}",
                                        erro != null ? erro.getMessage() : "");
                                return;
                            }
                            // InterruptedException / "interrompido" = cancelamento limpo
                            if (isCancelamento(erro)) {
                                log.info("SSE Tutor: streaming cancelado");
                                safeComplete(emitter);
                                return;
                            }
                            falhar(emitter, clienteLigado, erro);
                        }
                );
            } finally {
                heartbeat.cancel(false);
            }
        });
        workerRef.set(worker);

        return emitter;
    }

    private static boolean isCancelamento(Throwable erro) {
        if (erro == null) {
            return false;
        }
        if (erro instanceof InterruptedException) {
            return true;
        }
        String msg = erro.getMessage();
        if (msg != null && msg.toLowerCase().contains("interrompid")) {
            return true;
        }
        Throwable c = erro.getCause();
        return c instanceof InterruptedException;
    }

    private void enviarEvento(SseEmitter emitter, AtomicBoolean clienteLigado, String nomeEvento, String dados) {
        if (!clienteLigado.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(nomeEvento).data(dados));
        } catch (IOException e) {
            // ClientAbortException / broken pipe
            log.debug("SSE Tutor: falha ao enviar '{}' — cliente provavelmente fechou: {}",
                    nomeEvento, e.getMessage());
            clienteLigado.set(false);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("SSE Tutor: falha ao enviar evento '{}': {}", nomeEvento, e.getMessage());
            clienteLigado.set(false);
            Thread.currentThread().interrupt();
        }
    }

    private void concluir(SseEmitter emitter, AtomicBoolean clienteLigado,
                          AtomicBoolean respostaPersistida, Consumer<String> aoConcluir,
                          String textoCompleto) {
        try {
            if (clienteLigado.get() && respostaPersistida.compareAndSet(false, true)) {
                String limpo = ResponseTextCleaner.removerParagrafosDuplicados(textoCompleto);
                aoConcluir.accept(limpo);
            }
            if (clienteLigado.get()) {
                enviarEvento(emitter, clienteLigado, "concluido", "true");
            }
            safeComplete(emitter);
        } catch (Exception e) {
            log.error("SSE Tutor: falha ao concluir/persistir", e);
            if (clienteLigado.get()) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void falhar(SseEmitter emitter, AtomicBoolean clienteLigado, Throwable erro) {
        log.error("SSE Tutor: erro durante streaming", erro);
        if (!clienteLigado.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("erro").data(codificarJson(
                    erro != null && erro.getMessage() != null ? erro.getMessage() : "Erro no Tutor IA")));
        } catch (Exception ignored) {
        }
        try {
            emitter.completeWithError(erro);
        } catch (Exception ignored) {
        }
        clienteLigado.set(false);
    }

    private static void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    /**
     * Codifica o texto como string JSON para preservar espaços nos tokens SSE.
     */
    private String codificarJson(String texto) {
        try {
            return objectMapper.writeValueAsString(texto == null ? "" : texto);
        } catch (Exception e) {
            log.warn("Falha ao codificar dados SSE como JSON: {}", e.getMessage());
            return texto;
        }
    }
}
