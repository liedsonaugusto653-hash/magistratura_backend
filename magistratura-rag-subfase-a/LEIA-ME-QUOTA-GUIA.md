# Patch — Monitorização de limites IA + Guia mais inteligente

## Conteúdo

### Backend
- `ia/IaQuotaState.java` (**novo**) — regista HTTP 429 / Retry-After dos providers
- `filter/IaRateLimitFilter.java` — snapshot de uso local + header Retry-After
- `controller/TutorController.java` — `GET /api/ia/status` enriquecido
- Providers Gemini / OpenAI-compatible / OpenRouter / Ollama — gravam 429
- `ia/ollama/OllamaConcurrencyLimiter.java` — activo com chat **ou** embeddings Ollama

### Frontend
- `guide/events.js` — `IA_RATE_LIMITED`, `IA_UNAVAILABLE`, `TUTOR_NO_CONTEXT`
- `guide/policy.js` — templates + cooldowns
- `stores/tutor.js` — estado de quota, notificações do guia
- `stores/questao.js` — prefill Tutor após resposta incorrecta
- `views/TutorView.vue` — badges `restantes/limite` e `limite API`

## Resposta de `/api/ia/status` (exemplo)

```json
{
  "provider": "OpenAI-compatible (llama-3.3-70b-versatile)",
  "disponivel": true,
  "localLimitEnabled": true,
  "localLimitPerMinute": 20,
  "localUsed": 3,
  "localRemaining": 17,
  "localResetAt": "2026-08-01T05:00:00Z",
  "upstreamRateLimited": false,
  "upstreamProvider": null,
  "retryAfterSeconds": null
}
```

## Como aplicar

Na raiz do projecto (pasta com `backend/` e `frontend/`):

```bash
unzip -o magistratura-quota-guia.zip
```

Ou copia as pastas `backend/src` e `frontend/src` por cima.

Reinicia backend e frontend.

## Nota

Este patch **não** consulta dashboards externos (Groq/Google). Monitoriza:
1. rate limit **local** da app;
2. último **429** reportado pelo provider upstream.
