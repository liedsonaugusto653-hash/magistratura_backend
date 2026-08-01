-- Caminhada / Jornada — progresso por utilizador (sem inventar legislação).
-- Conteúdo narrativo vive no frontend (seed versionado); a BD só guarda onde o utilizador ficou.

CREATE TABLE IF NOT EXISTS utilizador_jornada_progresso (
    utilizador_id   UUID PRIMARY KEY REFERENCES utilizadores(id) ON DELETE CASCADE,
    momento_id      VARCHAR(64),
    cena_id         VARCHAR(64),
    concluidos_json TEXT,
    actualizado_em  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE utilizador_jornada_progresso IS 'Progresso narrativo da Caminhada (João); não é LMS.';
