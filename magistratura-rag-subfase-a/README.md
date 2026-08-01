# Magistratura — pacote completo: auditoria + correção pipeline + locks + monitor

**Data:** 2026-07-28  
**Conteúdo de toda a sessão de trabalho** (não apenas lock timeout).

## Problema resolvido

Upload OK → `POST .../processar` 200 → documento preso em `PROCESSANDO` / `"A iniciar processamento…"` / `0%`.

## O que este ZIP inclui

| Área | Ficheiros |
|------|-----------|
| **Correcção TX / deadlock** | `DocumentoEstadoService`, `DocumentPipelineOrchestrator` |
| **Job + afterCommit** | `DocumentoProcessamentoJob`, `DocumentoService` |
| **Progresso seguro** | `DocumentoProgressoService` |
| **Timeouts PostgreSQL** | `application.yml` (lock_timeout, deadlock_timeout) |
| **Monitor de locks** | `sql/monitor_locks*.sql`, `kill_blocking.sql` |
| **Documentação** | Relatório de auditoria, fluxo antes/depois, COMO_APLICAR |

## Estrutura

```
magistratura-pipeline-completo/
├── README.md
├── COMO_APLICAR.md
├── env-lock-timeout.snippet
├── docs/
│   ├── RELATORIO_AUDITORIA_PIPELINE.md
│   └── FLUXO_ANTES_DEPOIS.md
├── backend/src/main/
│   ├── java/ao/magistratura/
│   │   ├── pipeline/DocumentPipelineOrchestrator.java
│   │   └── service/
│   │       ├── DocumentoEstadoService.java
│   │       ├── DocumentoProcessamentoJob.java
│   │       ├── DocumentoProgressoService.java
│   │       └── DocumentoService.java
│   └── resources/application.yml
└── sql/
    ├── monitor_locks.sql
    ├── monitor_locks_live.sql
    ├── kill_blocking.sql
    └── README_MONITOR_LOCKS.md
```

## Como colar no sistema

1. Descompacta o ZIP.
2. Copia a pasta `backend/src/main/java/ao/magistratura/...` por cima do teu `backend/src/main/java/ao/magistratura/...`.
3. Faz **merge** de `backend/src/main/resources/application.yml` (bloco `spring.datasource.hikari` + `app.pipeline.lock-timeout`) — se sobrescreveres o yml inteiro, confirma que não perdes outras chaves locais.
4. Apaga cópias soltas na **raiz** de `backend/` (`DocumentoService.java`, `DocumentoProcessamentoJob.java`, etc.) se existirem.
5. Reinicia o backend.
6. Opcional — reset de documentos presos (ver `COMO_APLICAR.md`).

## Ordem de leitura recomendada

1. `docs/RELATORIO_AUDITORIA_PIPELINE.md` — causa raiz e cadeia de chamadas  
2. `docs/FLUXO_ANTES_DEPOIS.md` — visão rápida  
3. `COMO_APLICAR.md` — passos e validação  
4. `sql/README_MONITOR_LOCKS.md` — monitorização em produção/dev  

## Variáveis de ambiente (opcional)

```bash
DB_LOCK_TIMEOUT=8s
DB_DEADLOCK_TIMEOUT=1s
DB_CONNECTION_TIMEOUT_MS=30000
DB_POOL_SIZE=10
```
