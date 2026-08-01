-- Snapshot rápido (uma query) — útil em loop:
--   watch -n 2 'psql -U magistratura -d magistratura -f monitor_locks_live.sql'

SELECT
    a.pid,
    a.state,
    CASE WHEN l.granted THEN 'HOLD' ELSE 'WAIT' END AS lock_status,
    l.mode,
    COALESCE(c.relname, l.locktype::text) AS target,
    a.wait_event_type,
    a.wait_event,
    ROUND(EXTRACT(EPOCH FROM (NOW() - a.xact_start))::numeric, 1) AS xact_s,
    ROUND(EXTRACT(EPOCH FROM (NOW() - a.query_start))::numeric, 1) AS query_s,
    LEFT(a.query, 90) AS query
FROM pg_stat_activity a
LEFT JOIN pg_locks l ON l.pid = a.pid
LEFT JOIN pg_class c ON c.oid = l.relation
WHERE a.datname = current_database()
  AND a.pid <> pg_backend_pid()
  AND a.state <> 'idle'
ORDER BY
    CASE WHEN l.granted IS FALSE THEN 0 ELSE 1 END,
    a.xact_start NULLS LAST;
