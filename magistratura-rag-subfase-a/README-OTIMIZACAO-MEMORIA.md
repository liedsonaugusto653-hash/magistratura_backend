# Optimização de memória — Magistratura Backend (Render Free 512 MB)

## 1. Diagnóstico das causas de OOM

| Causa | Impacto | Evidência no código |
|-------|---------|---------------------|
| **OCR / PDFBox** | Crítico | `OcrExtractorService` rasteriza páginas a **250 DPI em RGB** (~3–8 MB por página A4). Sem limite de páginas (`max-pages: 0`). Imagens não eram libertadas explicitamente. Vários jobs em paralelo (pool 2–4) multiplicam o pico. |
| **Upload 50 MB** | Alto | `spring.servlet.multipart.max-file-size: 50MB` — o PDF inteiro entra na heap + raster OCR. |
| **HikariCP pool=10** | Médio | 10 conexões JDBC + buffers nativos num container de 512 MB. |
| **Cache RAG size=512** | Médio | Cada entrada pode guardar extratos jurídicos longos. |
| **Async pipeline 2–4 threads** | Alto | Vários OCR simultâneos → pico de RAM multiplicado. |
| **JVM sem GC adequado** | Médio | Só `-Xmx350m`; sem SerialGC, sem ExitOnOutOfMemoryError, metaspace 128m generoso. |
| **Tomcat threads default** | Baixo–médio | Default ~200 threads = stacks reservadas. |

Funcionalidades **já bem desenhadas** (não mexidas):
- `ConversationMemory.JANELA_CONTEXTO = 8`
- `ContextAssemblyService` + `StudyContextPolicy` com truncagem
- `OllamaConcurrencyLimiter`
- Knowledge/RAG sob demanda (não carrega modelos na JVM)
- Embeddings via HTTP (Ollama remoto / fail-soft)

---

## 2. Ficheiros alterados

```
backend/Dockerfile
backend/src/main/resources/application.yml
backend/src/main/resources/application-prod.yml
backend/src/main/resources/application-cloud.yml
backend/src/main/java/ao/magistratura/config/AsyncConfig.java
backend/src/main/java/ao/magistratura/config/CacheConfig.java
backend/src/main/java/ao/magistratura/controller/HealthController.java   ← NOVO
backend/src/main/java/ao/magistratura/security/SecurityConfig.java
backend/src/main/java/ao/magistratura/service/pdf/OcrExtractorService.java
```

**Não alterados:** entidades, repositórios, APIs públicas de negócio, frontend, autenticação JWT, Flyway, estrutura de pacotes.

---

## 3. Explicação de cada mudança

### Dockerfile
- `-Xms64m -Xmx280m` — deixa ~40 MB para native/OS (Tesseract, PDFBox, OS, threads).
- `-XX:MaxMetaspaceSize=192m` / `-XX:ReservedCodeCacheSize=48m` — evita crescimento descontrolado.
- **`-XX:+UseSerialGC`** — GC com menor overhead para heaps pequenos (recomendado < 400 MB).
- `-XX:+ExitOnOutOfMemoryError` — o Render reinicia o serviço em vez de o deixar “zumbi”.
- `-Djava.awt.headless=true` — sem display AWT no container.
- `exec java ...` — PID 1 correcto para sinais do container.

### application.yml / prod / cloud
- Hikari: **pool 3** (prod/cloud), min idle 1, timeouts mais curtos, leak detection.
- Upload: **12–15 MB** (ainda cobre diplomas típicos).
- OCR: **dpi 150**, **max-pages 40**.
- Cache RAG: **64 / 48** entradas, TTL 60–90 s.
- Tomcat: **max 40–50** threads.
- Actuator: só `health,info` expostos.

### AsyncConfig
- **1 thread** de pipeline por omissão (`app.async.pipeline-*-size=1`).
- `CallerRunsPolicy` se a fila encher — não cria threads extra.
- Log de OOM em jobs async.

### OcrExtractorService
- Raster **GRAY** em vez de RGB (~⅓ da memória).
- `image.flush()` + null após cada página.
- Flush das imagens do pré-processamento.
- DPI efectivo limitado 100–300; default 150.
- Default `max-pages` 40.

### HealthController — `GET /api/health`
- Público, sem DB, devolve `status: UP` + heap usado/max.
- Render Health Check Path: `/api/health` (ou `/actuator/health`).

### SecurityConfig
- `/api/health` nas rotas públicas.

---

## 4. Como aplicar

1. Copia os ficheiros deste patch por cima do backend (mesma árvore de pastas).
2. Commit + push para o GitHub ligado ao Render.
3. No Render → Web Service → **Settings**:
   - **Health Check Path**: `/api/health`
   - **Environment**:
     ```
     SPRING_PROFILES_ACTIVE=cloud   # ou prod
     DB_POOL_SIZE=3
     PIPELINE_OCR_DPI=150
     PIPELINE_OCR_MAX_PAGES=40
     UPLOAD_MAX_FILE_SIZE=12MB
     ```
4. Redeploy (clear build cache se o Dockerfile não for detectado).

### Variáveis úteis (opcional)

| Variável | Default free | Descrição |
|----------|--------------|-----------|
| `DB_POOL_SIZE` | 3 | Conexões Hikari |
| `PIPELINE_OCR_DPI` | 150 | Resolução OCR |
| `PIPELINE_OCR_MAX_PAGES` | 40 | Páginas máximas OCR |
| `PIPELINE_OCR_ENABLED` | true | Desligar OCR se não precisares |
| `UPLOAD_MAX_FILE_SIZE` | 12MB | Tamanho máximo PDF |
| `CACHE_RAG_MAX_SIZE` | 48 | Entradas cache RAG |
| `TOMCAT_MAX_THREADS` | 40 | Threads HTTP |
| `app.async.pipeline-max-size` | 1 | Jobs pipeline em paralelo |

---

## 5. Compatibilidade Docker / Render

- Multi-stage Maven + JRE Alpine mantido.
- Tesseract + `por.traineddata` mantidos.
- `PORT` injectado pelo Render.
- Health check responde 200 sem autenticação.
- APIs existentes e contratos OpenAPI inalterados.
- Frontend continua a usar os mesmos endpoints.

---

## 6. Expectativa operacional

- **Arranque:** ~150–220 MB heap committed.
- **Repouso:** estável abaixo de ~280 MB total container.
- **1 OCR de diploma (~20 pág., 150 DPI GRAY):** pico controlado; jobs enfileiram-se (1 de cada vez).
- **Vários utilizadores** em chat/questões/biblioteca: leve; o gargalo continua a ser OCR/PDF, agora serializado.
- Se ainda houver OOM em PDF enorme: reduz `PIPELINE_OCR_MAX_PAGES` ou desactiva OCR no cloud e processa só PDFs com texto nativo.

---

## 7. O que NÃO foi feito (de propósito)

- Remover Tess4J / PDFBox / actuator / swagger.
- Mudar schema DB ou migrations.
- Alterar contratos REST ou o frontend.
- Carregar modelos de IA na JVM (já são HTTP remoto).

---

## 8. Ajuste pós-deploy: OutOfMemoryError: Metaspace

Após o primeiro deploy do patch, o serviço arrancou (~198 s) mas caiu com:

```
java.lang.OutOfMemoryError: Metaspace
```

**Causa:** `-XX:MaxMetaspaceSize=96m` era insuficiente para carregar todas as classes
(Spring Boot, Security, Hibernate, 28 repositories, Flyway, Actuator, RAG, PDF/OCR).

**Correção aplicada no Dockerfile:**

| Parâmetro | Antes | Depois |
|-----------|-------|--------|
| `-Xmx` | 320m | **280m** |
| `-XX:MaxMetaspaceSize` | 96m | **192m** |

Orçamento no container de 512 MB:

```
Heap       280 MB
Metaspace  192 MB
Code cache  48 MB
Sistema    ~40 MB
------------------
Total      ~512 MB
```

O aviso `Standard Commons Logging discovery... remove commons-logging.jar` é cosmético e não causa OOM.

---

## 9. Segundo OOM Metaspace (após subir para 192m)

O serviço chegou a `Started … in 199s` e caiu de imediato com `OutOfMemoryError: Metaspace`.
O log confirma que o código novo está activo (`pipelineExecutor: core=1, max=1`).

### Correção v3

| Parâmetro | v2 | v3 |
|-----------|----|----|
| `-Xmx` | 280m | **240m** |
| `-XX:MaxMetaspaceSize` | 192m | **220m** |
| `-XX:MetaspaceSize` | (default) | **128m** |
| `-XX:CompressedClassSpaceSize` | (default) | **96m** |

### Redução de classes no perfil cloud/prod

- `spring.main.lazy-initialization: true` — beans sob demanda
- `springdoc.*.enabled: false` — Swagger/OpenAPI desligado (poupa Metaspace)

### Checklist no painel do Render (importante)

1. **Environment variables** — apaga se existirem:
   - `JAVA_TOOL_OPTIONS`
   - `JAVA_OPTS`
   - qualquer flag `-XX:MaxMetaspaceSize=…` antiga
   
   O log `Picked up JAVA_TOOL_OPTIONS:` indica que o Render (ou uma env var) está a injectar opções. Se aí estiver o `96m` antigo, sobrescreve o Dockerfile.

2. **Clear build cache** + **Manual Deploy** (Rebuild).

3. Health Check Path: `/api/health` ou `/actuator/health`.

4. Para reactivar Swagger temporariamente: `SPRINGDOC_ENABLED=true`.
