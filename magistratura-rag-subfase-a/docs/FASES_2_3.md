# Ontologia — Fases 2 e 3

## Fase 2 — UI Mapa Jurídico

| Ficheiro | Função |
|----------|--------|
| `frontend/src/views/MapaJuridicoView.vue` | Lista entidades, mapa, tópicos, artigos, relações |
| `frontend/src/services/ontologiaService.js` | Cliente HTTP `/api/ontologia` |
| `frontend/src/stores/ontologia.js` | Estado Pinia |
| `frontend/src/router/index.js` | Rota `/mapa` + `meta.nav` (order 25) |
| `frontend/src/config/navIcons.js` | Ícone `Network` (`mapa`) |

Fluxo: menu **Mapa Jurídico** → entidade → tópico → artigos / relações → **Perguntar ao Tutor** (prefill `topicoId`).

## Fase 3 — Knowledge Layer + Tutor

| Ficheiro | Alteração |
|----------|-----------|
| `KnowledgeQuery.java` | Campo `topicoId` + factories `porTopico` / `juridicoComTopico` |
| `DefaultKnowledgeService.java` | Passagens da ontologia; fusão com RAG + boost |
| `ChatRequest.java` | Campo `topicoId` |
| `TutorService.java` | `KnowledgeQuery.juridicoComTopico(..., request.topicoId(), ...)` |
| `tutorService.js` / `stores/tutor.js` / `TutorView.vue` | Envio e prefill de `topicoId` |

### Comportamento RAG

1. Só `topicoId` (sem texto) → artigos ligados ao tópico (`estrategia=ONTOLOGIA`).
2. Texto + `topicoId` → retrieval habitual **fundido** com artigos do tópico (score boost).
3. Sem ligações no tópico → comportamento anterior (só embeddings/lexical).

## Popular o mapa

Enquanto não há UI de ligação:

```http
POST /api/ontologia/topicos/{topicoId}/artigos
{ "artigoId": "<uuid>", "relevancia": 1.0 }
```

Depois o mapa e o Tutor passam a usar esses artigos.
