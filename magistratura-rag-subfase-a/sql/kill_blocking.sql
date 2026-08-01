-- CUIDADO: cancela backends que estão a BLOQUEAR outros há > 60s.
-- Rever o SELECT antes de descomentar o pg_cancel_backend.

-- 1) Ver bloqueadores longos
SELECT
    blocking.pid AS blocking_pid,
    NOW() - blocked.query_start AS blocked_for,
    LEFT(blocking.query, 120) AS blocking_query,
    LEFT(blocked.query, 120) AS blocked_query
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
  AND NOW() - blocked.query_start > INTERVAL '60 seconds';

-- 2) Cancelar (SIGINT) — descomentar se necessário:
-- SELECT pg_cancel_backend(blocking_pid) FROM ( ... mesma query ... ) s;

-- 3) Terminar à força (SIGTERM) — último recurso:
-- SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid = <PID>;
