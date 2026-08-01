# SSE reativo + resiliência

## Objectivos

1. Heartbeat a cada **15 s** (Tutor + progresso de documentos)
2. Cancelamento imediato do worker quando o cliente fecha a aba/ligação
3. Headers anti-buffer para Nginx/proxies
4. Sem vazamento de threads nem persistência de respostas a meio quando o cliente aborta
5. Eventos tipados: `token` | `fontes` | `heartbeat` | `concluido` | `erro` (Tutor)  
   e `progress` | `done` | `error` | `heartbeat` (documentos)

## Ficheiros

| Ficheiro | Alteração |
|----------|-----------|
| `ia/StreamingService.java` | Heartbeat, `AtomicBoolean` cliente, interrupt do worker, fontes, sem persistir se desligado |
| `ia/ollama/OllamaProvider.java` | Loop NDJSON respeita `Thread.interrupted()` e fecha o body |
| `controller/TutorController.java` | `Cache-Control`, `X-Accel-Buffering: no`, `Connection: keep-alive` |
| `controller/DocumentoController.java` | Idem no endpoint `/progress` |
| `service/DocumentoProgressHub.java` | Heartbeat por emitter + cleanup no onError/timeout |
| `frontend/.../tutorService.js` | Ignora `heartbeat`; trata `fontes` |
| `frontend/.../documentoProgressSse.js` | Ignora `heartbeat` |

## Protocolo Tutor (`POST /api/ia/chat/stream`)

```
event: fontes
data: [{...}]

event: heartbeat
data: {"ts":1730000000000}

event: token
data: " palavra"

event: concluido
data: true
```

Se o cliente fechar a meio: o backend interrompe a virtual thread, **não** grava a mensagem parcial da IA, e deixa de enviar tokens.

## Nginx (exemplo)

```nginx
location /api/ia/chat/stream {
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 360s;
}
```

(`X-Accel-Buffering: no` já é enviado pela app.)

## Validação manual

1. Abrir Tutor, enviar pergunta longa — tokens a fluir.
2. Fechar o separador a meio — no log: `cliente desligado — a interromper worker` e `Ollama stream cancelado`.
3. Confirmar que **não** aparece mensagem IA incompleta ao reabrir a conversa.
4. Importar PDF e abrir progresso — heartbeats silenciosos; UI só reage a `progress`/`done`/`error`.
