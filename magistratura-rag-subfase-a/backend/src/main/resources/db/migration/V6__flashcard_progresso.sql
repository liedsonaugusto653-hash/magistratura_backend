-- Progresso individual de flashcards por estudante
-- Um registo por (utilizador, flashcard)

CREATE TABLE flashcard_progresso (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id     UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    flashcard_id      UUID NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    vezes_revisto     INTEGER NOT NULL DEFAULT 0,
    acertos           INTEGER NOT NULL DEFAULT 0,
    erros             INTEGER NOT NULL DEFAULT 0,
    nivel_dificuldade VARCHAR(15) NOT NULL DEFAULT 'MEDIO',
    ultima_revisao    TIMESTAMPTZ,
    proxima_revisao   TIMESTAMPTZ,
    CONSTRAINT uq_flashcard_progresso_utilizador_flashcard UNIQUE (utilizador_id, flashcard_id)
);

CREATE INDEX idx_flashcard_progresso_utilizador ON flashcard_progresso (utilizador_id);
CREATE INDEX idx_flashcard_progresso_flashcard  ON flashcard_progresso (flashcard_id);
CREATE INDEX idx_flashcard_progresso_proxima    ON flashcard_progresso (proxima_revisao);
