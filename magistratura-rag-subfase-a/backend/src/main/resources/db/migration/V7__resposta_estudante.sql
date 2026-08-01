-- Histórico de respostas do estudante a questões individuais (fora de simulado)

CREATE TABLE resposta_estudante (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id        UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    questao_id           UUID NOT NULL REFERENCES questoes(id) ON DELETE CASCADE,
    resposta_escolhida   VARCHAR(1) NOT NULL,
    correta              BOOLEAN NOT NULL,
    data_resposta        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resposta_estudante_utilizador ON resposta_estudante (utilizador_id);
CREATE INDEX idx_resposta_estudante_questao    ON resposta_estudante (questao_id);
CREATE INDEX idx_resposta_estudante_data       ON resposta_estudante (data_resposta DESC);
