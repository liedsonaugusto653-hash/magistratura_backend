# Deploy cloud — Magistratura (Vercel + Render + Neon + Groq)

Arquitectura:

```
Utilizador → Vercel (Vue) → Render (Spring Boot) → Neon (PostgreSQL)
                                              └→ Groq API (LLM)
```

A chave Groq **nunca** vai para o frontend.

---

## 0. Pré-requisitos

- Conta [GitHub](https://github.com) com o código do projecto
- [Neon](https://neon.tech) — PostgreSQL
- [Render](https://render.com) — backend
- [Vercel](https://vercel.com) — frontend
- [Groq](https://console.groq.com) — API key (`gsk_...`)

Estrutura esperada no repo:

```
magistratura-rag-subfase-a/
  backend/
  frontend/
  deploy/
```

Aplica antes os patches úteis (quota/guia, prompt tutor, V32, etc.) no código local e faz **push** para o GitHub.

---

## 1. Neon (base de dados)

1. Create project → copia **host**, **database**, **user**, **password**.
2. SQL Editor (opcional): `CREATE EXTENSION IF NOT EXISTS vector;`  
   (só necessário se mais tarde activares `KNOWLEDGE_VECTOR_STORE=pgvector`)
3. Guarda os valores para o Render.

---

## 2. Render (backend)

### Opção A — Docker (recomendado)

1. **New → Web Service** → liga o repositório GitHub.
2. Root directory: deixa vazio se o repo for o monorepo;  
   **Dockerfile path:** `backend/Dockerfile`  
   **Docker context:** `backend`
3. Instance: **Free**.
4. Environment → cola as variáveis de `deploy/env.backend.render.example`  
   (substitui valores reais; `CORS`/`FRONTEND_BASE_URL` podes actualizar após a Vercel).
5. Deploy.

### Opção B — Blueprint

Na raiz do repo (se tiveres `render.yaml` na raiz do GitHub):

- Dashboard → **New Blueprint** → selecciona o repo.

### Verificar backend

- Logs: `Started Magistratura...` e Flyway OK.
- Abre: `https://SEU-SERVICO.onrender.com/api/ia/status`  
  (pode pedir auth; se devolver 401, o servidor está vivo).

**Nota free tier:** o serviço “dorme”; o 1.º pedido após inactividade pode demorar ~30–60 s.

---

## 3. Vercel (frontend)

1. **Add New Project** → repo → **Root Directory:** `frontend`
2. Framework: Vite (auto)
3. Build: `npm run build` · Output: `dist`
4. Environment Variable:
   - `VITE_API_BASE_URL` = `https://SEU-SERVICO.onrender.com`  
     (sem `/api` no fim, ou com — o código aceita ambos)
5. Deploy.
6. Copia o URL `https://xxx.vercel.app`

### Actualizar CORS no Render

No Render, edita:

```text
CORS_ALLOWED_ORIGINS=https://xxx.vercel.app
FRONTEND_BASE_URL=https://xxx.vercel.app
```

Faz **Manual Deploy** / restart do backend.

---

## 4. Testes

1. Abre o site na Vercel → **Login** (`estudante@magistratura.local` / `123456` se o seed existir).
2. Tutor → badge com modelo Groq.
3. Pergunta: «Que dia é hoje?» (com o patch do prompt tutor).
4. Se 429: limite Groq — espera ou troca de modelo.

---

## 5. Profile `cloud` vs `prod`

| Profile | Uso |
|---------|-----|
| **`cloud`** | Free tier: Groq + Neon + vector `noop` + email logging |
| **`prod`** | Exige pgvector, indexer real e SMTP (ProductionStartupValidator) |

Para este guia usa sempre:

```text
SPRING_PROFILES_ACTIVE=cloud
```

---

## 6. Limitações do plano gratuito

| Componente | Limitação |
|------------|-----------|
| Render free | Sleep; disco efémero (PDFs podem perder-se no redeploy) |
| Neon free | Limites de storage/compute |
| Groq free | Rate limits (HTTP 429) |
| OCR | Desligado (`PIPELINE_OCR_ENABLED=false`) — Tesseract não vem na imagem slim |

---

## 7. Checklist rápido

- [ ] Neon criado e acessível
- [ ] `backend/Dockerfile` no repo
- [ ] Render com env vars (JWT, DB, Groq, CORS)
- [ ] Flyway correu sem erros
- [ ] Vercel com `VITE_API_BASE_URL`
- [ ] CORS actualizado com URL Vercel
- [ ] Login + Tutor funcionam

---

## 8. Problemas comuns

| Sintoma | Causa provável |
|---------|----------------|
| Frontend carrega, API falha (CORS) | `CORS_ALLOWED_ORIGINS` sem URL exacta da Vercel |
| Login OK, Tutor falha | `OPENAI_API_KEY` em falta ou 429 Groq |
| Arranque FATAL vector-store | Estás em profile `prod` — muda para `cloud` |
| SSL Neon | `DB_SSLMODE=require` |
| 1.º pedido muito lento | Render a acordar do sleep |

---

## Ficheiros deste pacote

```
DEPLOY_CLOUD.md
render.yaml
backend/Dockerfile
backend/src/main/resources/application-cloud.yml
frontend/vercel.json
frontend/.env.production.example
frontend/src/api/http.js
frontend/src/services/tutorService.js
frontend/src/services/documentoProgressSse.js
deploy/env.backend.render.example
deploy/env.frontend.vercel.example
```

Sobrepor na raiz do projecto (`magistratura-rag-subfase-a/`) e fazer commit/push antes do deploy.
