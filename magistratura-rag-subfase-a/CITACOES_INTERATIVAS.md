# Citações interactivas no Tutor IA

## Objectivo

O modelo cita fontes recuperadas pelo RAG com marcadores `[1]`, `[2]`, …  
O frontend torna cada marcador clicável e mostra o extrato jurídico correspondente.

## Fluxo

```
KnowledgeService.search
        │
        ▼
PromptBuilder  →  fontes numeradas no system prompt  →  LLM cita [n]
        │
        ▼
TutorService.mapearFontes  →  List<CitacaoFonteResponse>
        │
        ├─ chat síncrono  →  MensagemResponse.fontes
        └─ chat stream   →  evento SSE "fontes" (antes dos tokens)
                │
                ▼
Frontend: parse [n] na bolha + chips + painel de extrato
```

## Ficheiros alterados

| Ficheiro | Alteração |
|----------|-----------|
| `prompts/tutor.txt` | Regras de citação `[n]` |
| `ia/PromptBuilder.java` | Passagens numeradas `[1]…` no contexto |
| `dto/ia/CitacaoFonteResponse.java` | **Novo** DTO de fonte |
| `dto/ia/MensagemResponse.java` | Campo `fontes` |
| `ia/StreamingService.java` | Evento SSE `fontes` |
| `service/TutorService.java` | `mapearFontes` + serialização |
| `frontend/.../tutorService.js` | Callback `onFontes` |
| `frontend/.../stores/tutor.js` | Anexa `fontes` à mensagem |
| `frontend/.../views/TutorView.vue` | Marcadores clicáveis + painel |

## Protocolo SSE (extra)

```
event: fontes
data: [{"n":1,"artigoId":"...","diplomaTitulo":"...","extrato":"...","score":0.91},...]

event: token
data: "Segundo"

event: token
data: " o"

...

event: concluido
data: true
```

## UX

- No texto da IA, `[1]` é um botão.
- Abaixo da bolha: chips ` [1] Art. 36 `.
- Clique abre painel com extrato + link «Abrir artigo na Biblioteca».
- Mensagens históricas sem metadados ainda mostram o marcador (tooltip genérico).

## Notas

- A ordem de `fontes` **deve** coincidir com a numeração do `PromptBuilder`.
- Não há migration: as fontes não são persistidas na tabela `mensagens_ia` nesta entrega (só na resposta em tempo real / objecto em memória no Pinia).
- Correção colateral: `rotuloArtigo` em `TutorView.vue` estava corrompido no ZIP original e foi reparado.

## Persistência (V27)

As fontes passam a ser gravadas em `mensagens_ia.fontes_json` (TEXT / JSON array).

| Ficheiro | Alteração |
|----------|-----------|
| `V27__mensagens_ia_fontes.sql` | `ADD COLUMN fontes_json TEXT` |
| `entity/MensagemIa.java` | Campo `fontesJson` |
| `ia/ConversationMemory.java` | `registarMensagemIA(..., fontesJson)` |
| `service/TutorService.java` | Grava no chat sync/stream; desserializa ao listar conversa |

Ao reabrir uma conversa (`GET /api/ia/conversas/{id}`), cada `MensagemResponse` inclui `fontes` e o frontend continua a renderizar marcadores `[n]` e o painel.

Mensagens antigas (sem coluna preenchida) devolvem `fontes: []` — os marcadores no texto continuam visíveis, mas sem extrato metadados.
