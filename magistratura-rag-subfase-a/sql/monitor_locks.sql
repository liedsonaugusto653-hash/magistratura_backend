-- =============================================================================
-- Magistratura — monitor de locks PostgreSQL
-- Uso: psql -U magistratura -d magistratura -f monitor_locks.sql
--   ou: psql ... -c "\i monitor_locks.sql"
-- =============================================================================

\echo '=== 1. Sessões activas (resumo) ==='
SELECT
    pid,
    usename,
    application_name AS app,
    state,
    wait_event_type,
    wait_event,
    LEFT(query, 80) AS query_preview,
    NOW() - query_start AS query_age,
    NOW() - state_change AS state_age
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid <> pg_backend_pid()
ORDER BY query_start NULLS LAST;

\echo ''
\echo '=== 2. Locks em espera (bloqueados) ==='
SELECT
    blocked.pid              AS blocked_pid,
    blocked.usename          AS blocked_user,
    LEFT(blocked.query, 100) AS blocked_query,
    blocking.pid             AS blocking_pid,
    blocking.usename         AS blocking_user,
    LEFT(blocking.query, 100) AS blocking_query,
    NOW() - blocked.query_start AS blocked_for
FROM pg_stat_activity blocked
JOIN pg_locks bl ON bl.pid = blocked.pid AND NOT bl.granted
JOIN pg_locks gl ON gl.locktype = bl.locktype
                 AND gl.database IS NOT DISTINCT FROM bl.database
                 AND gl.relation IS NOT DISTINCT FROM bl.relation
                 AND gl.page IS NOT DISTINCT FROM bl.page
                 AND gl.tuple IS NOT DISTINCT FROM bl.tuple
                 AND gl.virtualxid IS NOT DISTINCT FROM bl.virtualxid
                 AND gl.transactionid IS NOT DISTINCT FROM bl.transactionid
                 AND gl.classid IS NOT DISTINCT FROM bl.classid
                 AND gl.objid IS NOT DISTINCT FROM bl.objid
                 AND gl.objsubid IS NOT DISTINCT FROM bl.objsubid
                 AND gl.pid <> bl.pid
                 AND gl.granted
JOIN pg_stat_activity blocking ON blocking.pid = gl.pid
WHERE blocked.datname = current_database()
ORDER BY blocked_for DESC NULLS LAST;

\echo ''
\echo '=== 3. Locks na tabela documentos (e índices) ==='
SELECT
    l.pid,
    a.usename,
    a.state,
    l.mode,
    l.granted,
    c.relname AS relation,
    LEFT(a.query, 100) AS query_preview,
    NOW() - a.query_start AS age
FROM pg_locks l
JOIN pg_class c ON c.oid = l.relation
JOIN pg_stat_activity a ON a.pid = l.pid
WHERE c.relname IN ('documentos', 'artigos', 'documento_embeddings')
   OR c.relname LIKE 'documentos%'
ORDER BY l.granted, age DESC NULLS LAST;

\echo ''
\echo '=== 4. Transacções abertas há mais de 5s ==='
SELECT
    pid,
    usename,
    state,
    xact_start,
    NOW() - xact_start AS xact_age,
    NOW() - query_start AS query_age,
    wait_event_type,
    wait_event,
    LEFT(query, 120) AS query_preview
FROM pg_stat_activity
WHERE datname = current_database()
  AND xact_start IS NOT NULL
  AND pid <> pg_backend_pid()
  AND NOW() - xact_start > INTERVAL '5 seconds'
ORDER BY xact_start;

\echo ''
\echo '=== 5. Contagem de locks por relação ==='
SELECT
    COALESCE(c.relname, l.locktype::text) AS target,
    l.mode,
    COUNT(*) FILTER (WHERE l.granted)     AS granted,
    COUNT(*) FILTER (WHERE NOT l.granted) AS waiting
FROM pg_locks l
LEFT JOIN pg_class c ON c.oid = l.relation
WHERE l.database = (SELECT oid FROM pg_database WHERE datname = current_database())
   OR l.database IS NULL
GROUP BY 1, 2
HAVING COUNT(*) FILTER (WHERE NOT l.granted) > 0
    OR COUNT(*) > 5
ORDER BY waiting DESC, granted DESC;

\echo ''
\echo '=== 6. lock_timeout / deadlock_timeout da sessão actual ==='
SHOW lock_timeout;
SHOW deadlock_timeout;
