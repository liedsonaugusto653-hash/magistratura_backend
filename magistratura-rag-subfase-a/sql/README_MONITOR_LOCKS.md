# Monitorar locks activos — Magistratura / PostgreSQL

## Pré-requisito

```bash
# Docker Compose do projecto
docker exec -it magistratura-postgres psql -U magistratura -d magistratura

# Ou local
psql -h localhost -U magistratura -d magistratura
```

## Scripts

| Ficheiro | Uso |
|----------|-----|
| `monitor_locks.sql` | Relatório completo (sessões, waits, `documentos`, TX longas) |
| `monitor_locks_live.sql` | Snapshot numa query — bom para `watch` |
| `kill_blocking.sql` | Identificar (e opcionalmente cancelar) bloqueadores > 60s |

```bash
psql -U magistratura -d magistratura -f monitor_locks.sql

# Actualização a cada 2s
watch -n 2 'psql -U magistratura -d magistratura -At -f monitor_locks_live.sql'
```

## O que procurar no bug do pipeline

1. **Secção 2 / WAIT** — uma sessão em `UPDATE documentos` (progresso) e outra com TX aberta desde o job do pipeline.
2. **Secção 3** — locks em `documentos` com `granted = false`.
3. **Secção 4** — `xact_age` de minutos no thread `pipeline-*` → TX longa (bug estrutural).
4. **Secção 6** — `lock_timeout` deve ser `8s` (ou o valor de `DB_LOCK_TIMEOUT`) nas sessões da app; se for `0` / vazio, o `connection-init-sql` do Hikari não está a aplicar.

## Query mínima (colar no psql)

```sql
SELECT pid, state, wait_event, NOW() - xact_start AS xact_age, LEFT(query, 80)
FROM pg_stat_activity
WHERE datname = current_database() AND state <> 'idle' AND pid <> pg_backend_pid();
```

## Após o patch (sem TX longa + lock_timeout)

- Não deve haver waits longos em `documentos` durante OCR.
- Se houver wait, deve terminar em ≤ `lock_timeout` (default 8s) com erro `55P03`.
