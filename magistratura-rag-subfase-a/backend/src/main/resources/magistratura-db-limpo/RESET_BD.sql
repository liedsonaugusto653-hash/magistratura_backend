-- =====================================================================
-- RESET_BD.sql — Apagar e recriar schema (ambiente de desenvolvimento)
-- Executar como superuser na base magistratura:
--   psql -U postgres -d magistratura -f RESET_BD.sql
-- Depois: mvn spring-boot:run (Flyway aplica V1..V4)
-- =====================================================================

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO PUBLIC;
