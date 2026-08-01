# Magistratura — Instruções de Deploy

Documento de referência para pôr a plataforma no ar em **desenvolvimento local** e em **produção (Docker)**.

---

## 1. Requisitos

| Componente | Versão recomendada | Notas |
|------------|-------------------|--------|
| **Java** | 21 (Temurin) | Backend Spring Boot 3 |
| **Maven** | 3.9+ | Build do backend |
| **Node.js** | 20 LTS | Frontend Vue 3 + Vite |
| **PostgreSQL** | 16 | Flyway aplica migrações no arranque |
| **Ollama** | recente | Tutor IA (`llama3.2` ou `llama3.2:3b`) |
| **Tesseract OCR** | 5.x + `por` | PDFs digitalizados / protegidos |
| **Docker** (prod) | 24+ + Compose v2 | Opcional em dev |

### Sistema operativo

- **Windows**: desenvolvimento típico; Tesseract em `C:\Program Files\Tesseract-OCR`.
- **Linux / macOS**: Tesseract via `apt` / `brew`; `tessdata` com `por.traineddata`.

---

## 2. Variáveis de ambiente

### Backend (ficheiro `backend/.env` ou export no shell)

Copiar a partir de `backend/.env.example`:

```bash
cp backend/.env.example backend/.env
```

| Variável | Obrigatório | Descrição |
|----------|-------------|-----------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Sim | PostgreSQL |
| `JWT_SECRET` | Sim (prod) | ≥ 32 caracteres aleatórios; em `prod` o boot falha se for fraco |
| `JWT_EXPIRATION` | Não | ms (default 86400000 = 24 h) |
| `CORS_ALLOWED_ORIGINS` | Sim | Origens do frontend (ex. `http://localhost:5173`) |
| `FRONTEND_BASE_URL` | Sim (email reset) | URL pública do front (links de recuperação) |
| `OLLAMA_BASE_URL` | Sim (IA) | Ex. `http://localhost:11434` |
| `OLLAMA_MODEL` | Não | Default `llama3.2` |
| `OLLAMA_TIMEOUT` | Não | Segundos |
| `BIBLIOTECA_STORAGE_PATH` | Sim | Pasta dos PDFs (persistente) |
| `SPRING_PROFILES_ACTIVE` | Sim | `dev` ou `prod` |
| `SERVER_PORT` | Não | Default `8080` |
| `KNOWLEDGE_VECTOR_STORE` | Não | `noop` (default) ou `pgvector` |
| `PIPELINE_OCR_ENABLED` | Não | `true` / `false` |
| `PIPELINE_OCR_LANGUAGE` | Não | `por` |
| `PIPELINE_OCR_DATAPATH` | Condicional | Pasta `tessdata` (obrigatório se Tesseract não estiver no PATH) |
| `PIPELINE_OCR_DPI` | Não | Default `200` (não baixar em prod se quiseres qualidade) |
| `PIPELINE_OCR_THREADS` | Não | Default `4` — paralelismo OCR |

Gerar JWT forte:

```bash
openssl rand -base64 48
```

### Frontend (dev)

O Vite faz proxy de `/api` → `http://localhost:8080` (ver `frontend/vite.config.js`).  
Opcional: `VITE_API_PROXY_TARGET=http://localhost:8080`.

Em **produção** o nginx do contentor faz proxy de `/api` para o serviço `backend` — não é preciso URL absoluta no browser.

---

## 3. Deploy local (desenvolvimento)

### 3.1 PostgreSQL

**Opção A — Docker só da BD** (recomendado):

```bash
cd backend
docker compose up -d postgres
```

**Opção B — PostgreSQL instalado na máquina**, com base `magistratura` e utilizador alinhados ao `.env`.

### 3.2 Ollama (Tutor IA)

```bash
# instalar Ollama: https://ollama.com
ollama pull llama3.2
# ou: ollama pull llama3.2:3b
ollama serve   # se não estiver já a correr (porta 11434)
```

### 3.3 Tesseract (OCR)

**Windows**

1. Instalar [Tesseract](https://github.com/UB-Mannheim/tesseract/wiki) com idioma **Portuguese**.
2. No `.env`:

```env
PIPELINE_OCR_DATAPATH=C:/Program Files/Tesseract-OCR/tessdata
```

**Ubuntu / Debian**

```bash
sudo apt update
sudo apt install -y tesseract-ocr tesseract-ocr-por
# datapath típico: /usr/share/tesseract-ocr/5/tessdata
```

```env
PIPELINE_OCR_DATAPATH=/usr/share/tesseract-ocr/5/tessdata
```

### 3.4 Backend

```bash
cd backend
cp .env.example .env   # editar passwords e JWT
mkdir -p storage/documentos
mvn -B spring-boot:run
# ou: mvn -B -DskipTests package && java -jar target/*.jar
```

- API: `http://localhost:8080`  
- Swagger: `http://localhost:8080/swagger-ui.html`  
- Flyway corre **automaticamente** (inclui V25 perfil/progresso OCR se o patch UX estiver aplicado).

### 3.5 Frontend

```bash
cd frontend
npm ci || npm install
npm run dev
```

- App: `http://localhost:5173`  
- Login com conta criada em `/registo` ou utilizador já existente na BD.

### 3.6 Checklist rápido local

1. `GET http://localhost:8080/api/ia/status` → `disponivel: true` com Ollama no ar.  
2. Login no front → Dashboard.  
3. Importar PDF em **Importar Documentos** → Processar → ver progresso de páginas.  
4. **O meu perfil** / **Definições** → guardar.  
5. Tutor IA com contexto de diploma/artigo.

---

## 4. Deploy produção (Docker Compose)

Ficheiros em `deploy/`:

| Ficheiro | Função |
|----------|--------|
| `docker-compose.prod.yml` | Postgres + backend + frontend (nginx) |
| `Dockerfile.backend` | Build Maven → JRE 21 |
| `Dockerfile.frontend` | Build Vite → nginx |
| `nginx.conf` | SPA + proxy `/api` |
| `.env.example` | Modelo de secrets |

### 4.1 Preparar

```bash
cd deploy
cp .env.example .env
# Editar .env — JWT_SECRET obrigatório e forte
```

Exemplo mínimo de `.env`:

```env
DB_NAME=magistratura
DB_USER=magistratura
DB_PASSWORD=<password-forte>
JWT_SECRET=<openssl-rand-base64-48>
FRONTEND_BASE_URL=https://teu-dominio.exemplo
CORS_ALLOWED_ORIGINS=https://teu-dominio.exemplo
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=llama3.2:3b
KNOWLEDGE_VECTOR_STORE=noop
SPRING_PROFILES_ACTIVE=prod
```

> **Ollama**: o compose de produção **não** sobe Ollama por omissão. Corre Ollama no host ou descomenta o serviço `ollama` em `docker-compose.prod.yml` e ajusta `OLLAMA_BASE_URL`.

> **OCR no contentor**: a imagem JRE slim **não** inclui Tesseract. Opções:
> 1. Estender `Dockerfile.backend` com `apt-get install tesseract-ocr tesseract-ocr-por` e `PIPELINE_OCR_DATAPATH=/usr/share/tesseract-ocr/5/tessdata`; ou  
> 2. Processar PDFs textuais no servidor e reservar OCR para um worker com Tess4J no host.

### 4.2 Build e arranque

Na **raiz do repositório** (ou a partir de `deploy/` com paths relativos):

```bash
cd deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Serviços:

| Serviço | Porta host | URL |
|---------|------------|-----|
| frontend (nginx) | 80 | `http://localhost` |
| backend | 8080 | `http://localhost:8080` (também via `/api` no 80) |
| db | (interno) | só rede Docker |

### 4.3 Persistência

- Volume `pgdata` — PostgreSQL.  
- **PDFs**: monta um volume em `BIBLIOTECA_STORAGE_PATH` (recomendado). Exemplo a acrescentar ao serviço `backend` no compose:

```yaml
    environment:
      BIBLIOTECA_STORAGE_PATH: /data/documentos
    volumes:
      - biblioteca_data:/data/documentos
volumes:
  biblioteca_data:
```

### 4.4 HTTPS

Colocar **Caddy**, **Traefik** ou **nginx** no host à frente do porto 80, com certificados Let's Encrypt, e apontar `FRONTEND_BASE_URL` / `CORS_ALLOWED_ORIGINS` para `https://…`.

### 4.5 Actualizar versão

```bash
cd deploy
git pull   # ou copiar artefactos novos
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Flyway aplica migrações novas (ex. `V25`) no próximo arranque do backend — **não** apagar o volume `pgdata` em actualizações normais.

### 4.6 Parar / logs

```bash
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml down        # mantém volumes
docker compose -f docker-compose.prod.yml down -v    # APAGA dados da BD
```

---

## 5. Aplicar o pacote UX / pipeline (patch)

Se recebeste `magistratura-ux-premium.zip` (ou patches de pipeline):

```bash
# na raiz do projecto Magistratura
unzip -o magistratura-ux-premium.zip
cd backend && mvn -B -DskipTests package
cd ../frontend && npm ci && npm run build
# reiniciar backend / compose
```

Confirmar que existe `V25__perfil_preferencias_progresso.sql` em `backend/src/main/resources/db/migration/`.

---

## 6. CI (GitHub Actions)

O workflow `.github/workflows/ci.yml` já:

1. Sobe Postgres de teste e corre `mvn test` no backend.  
2. Faz `npm ci` + `npm run build` no frontend.

Em branches `main` / `master` / `develop`. Para deploy automático, acrescentar um job que faça push da imagem ou `compose` no servidor (fora do âmbito deste documento).

---

## 7. Segurança em produção

1. **`JWT_SECRET` único e longo** — nunca o valor de exemplo.  
2. **`SPRING_PROFILES_ACTIVE=prod`** — valida secret inseguro no boot.  
3. Password da BD forte; não expor porto 5432 publicamente.  
4. CORS só com o domínio real do frontend.  
5. Backups regulares do volume Postgres e da pasta de PDFs.  
6. Rate limit de login já previsto no código (filtro) — manter activo.

---

## 8. Resolução de problemas

| Sintoma | Causa provável | Acção |
|---------|----------------|--------|
| Backend não arranca (JWT) | Secret curto em `prod` | Gerar secret ≥ 32 chars |
| Flyway falha | Migração em falta / ordem | Ver logs; não saltar versões Vn |
| IA indisponível | Ollama off ou URL errada | `ollama serve`; testar `/api/ia/status` |
| 0 artigos em PDF imagem | OCR off / sem `por` | Instalar Tesseract + `PIPELINE_OCR_DATAPATH` |
| Timeout no browser ao processar | Pedido HTTP longo (versão antiga) | Confirmar processamento **async** + polling no front |
| Frontend 404 em rotas Vue | nginx sem `try_files` | Usar `deploy/nginx.conf` do repo |
| CORS bloqueado | Origem não listada | Ajustar `CORS_ALLOWED_ORIGINS` |
| PDFs “desaparecem” após recreate | Contentor sem volume de storage | Montar volume em `BIBLIOTECA_STORAGE_PATH` |

---

## 9. Ordem recomendada num servidor novo

1. Instalar Docker + Ollama (+ Tesseract se OCR no mesmo host).  
2. Clonar/copiar o código e patches UX.  
3. Configurar `deploy/.env`.  
4. `docker compose -f docker-compose.prod.yml up -d --build`.  
5. Abrir `http://servidor` → registo/login.  
6. Importar um diploma de teste → processar → Tutor.  
7. Configurar HTTPS e backups.

---

## 10. Contactos úteis no código

- Compose prod: `deploy/docker-compose.prod.yml`  
- Env exemplo prod: `deploy/.env.example`  
- Env exemplo dev: `backend/.env.example`  
- CI: `.github/workflows/ci.yml`  
- Storage PDFs: `app.biblioteca.storage-path` / `BIBLIOTECA_STORAGE_PATH`
