# Estás mesmo a subir as alterações? Checklist

## O que os teus logs PROVAM

| Sinal no log | Significado |
|--------------|-------------|
| `pipelineExecutor: core=1, max=1` | ✅ O **JAR** tem o `AsyncConfig` do patch |
| `profile is active: "cloud"` | ✅ Profile cloud activo |
| `Picked up JAVA_TOOL_OPTIONS:` | ⚠️ Há env `JAVA_TOOL_OPTIONS` (pode estar vazia) |
| **Não** aparece `=== JVM MEMORY ===` | ❌ Ainda **não** tens o `JvmMemoryReporter` (deploy incompleto ou build antigo) |
| OOM Metaspace logo após Tomcat | Flags de Metaspace provavelmente ainda baixas **ou** 512 MB não chega |

O JAR Java e o **Dockerfile (ENTRYPOINT)** são coisas diferentes.
Podes ter o código novo no jar e o container a arrancar ainda com `-XX:MaxMetaspaceSize=96m` se:

1. O `Dockerfile` no GitHub **não** foi actualizado / está noutro sítio
2. O Render **não** está a usar esse Dockerfile (Root Directory errado)
3. Build cache reutilizou a imagem antiga
4. Env vars no painel sobrescrevem as flags

---

## Passo 1 — Confirma a estrutura no GitHub

O Render precisa de ver o Dockerfile no sítio certo.

**Opção A — Root Directory = `backend`**
```
repo/
  backend/
    Dockerfile          ← tem de existir AQUI
    pom.xml
    src/...
```

**Opção B — Dockerfile na raiz do repo**
```
repo/
  Dockerfile            ← COPY paths têm de apontar para backend/
  backend/
    pom.xml
    src/...
```

No Render → Settings → **Root Directory**:
- Se o código está em `backend/`, Root Directory deve ser `backend`
- Environment: **Docker**

---

## Passo 2 — Força as flags no painel (mais fiável)

No Render → Environment, **cria** (não deixes vazio):

```
JAVA_TOOL_OPTIONS=-Xmx180m -XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=128m -XX:CompressedClassSpaceSize=100m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Dspring.jmx.enabled=false
```

Nota: o Java **acrescenta** `JAVA_TOOL_OPTIONS` aos argumentos do `java ...`.  
Se no ENTRYPOINT também houver `-Xmx200m` e `-XX:MaxMetaspaceSize=256m`, a **última** flag do mesmo tipo costuma ganhar (linha de comando depois de TOOL_OPTIONS, conforme a JVM). O importante é **nenhuma** ficar em 96m.

Se tiveres `JAVA_OPTS`, apaga — o Temurin não a usa por omissão; só gera confusão.

---

## Passo 3 — Clear Build Cache + Redeploy

1. Render → Manual Deploy → **Clear build cache & deploy**
2. Espera o build completo (não só “restart”)

---

## Passo 4 — Lê o log de arranque

Com este patch deves ver **no início**:

```text
[jvm] PORT=10000
[jvm] JAVA_TOOL_OPTIONS=-Xmx180m -XX:MaxMetaspaceSize=256m ...
```

E **depois do Started**:

```text
=== JVM MEMORY (confirma flags do container) ===
Heap max=200MB ...
Metaspace used=..MB max=256MB
================================================
```

### Interpretação

| Metaspace max no log | Conclusão |
|----------------------|-----------|
| `max=96` ou `max=192` | Dockerfile/env antigos — **não** subiu a v3/v4 |
| `max=256` | Flags correctas; se ainda OOM, o free tier é curto para este stack |
| Linha `=== JVM MEMORY ===` ausente | `JvmMemoryReporter` não está no jar — push incompleto |

---

## Passo 5 — Ficheiros mínimos a ter no Git

```
backend/Dockerfile
backend/src/main/resources/application-cloud.yml
backend/src/main/resources/application-prod.yml
backend/src/main/resources/application.yml
backend/src/main/java/ao/magistratura/config/AsyncConfig.java
backend/src/main/java/ao/magistratura/config/CacheConfig.java
backend/src/main/java/ao/magistratura/config/JvmMemoryReporter.java   ← NOVO
backend/src/main/java/ao/magistratura/controller/HealthController.java
backend/src/main/java/ao/magistratura/security/SecurityConfig.java
backend/src/main/java/ao/magistratura/service/pdf/OcrExtractorService.java
```

Confirma no GitHub (browser) que o conteúdo do `Dockerfile` tem `MaxMetaspaceSize=256m`.

---

## Se Metaspace max=256 e ainda cai

Aí já não é “não subiste as alterações”. É limite real do plano free com este monólito.
Opções:

1. Plano Render com mais RAM (recomendado para produção)
2. Separar OCR/pipeline para um worker
3. Remover springdoc do `pom.xml` em runtime cloud (já está disabled por config; remover a dependência poupa mais classes)
