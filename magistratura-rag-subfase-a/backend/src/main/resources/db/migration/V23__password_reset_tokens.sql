-- Tokens de recuperação de password. Um token por pedido; tokens antigos
-- do mesmo utilizador são apagados quando um novo é gerado (ver AuthService).
CREATE TABLE password_reset_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id   UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    token           VARCHAR(64) NOT NULL UNIQUE,
    expira_em       TIMESTAMPTZ NOT NULL,
    usado           BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_utilizador ON password_reset_tokens(utilizador_id);
