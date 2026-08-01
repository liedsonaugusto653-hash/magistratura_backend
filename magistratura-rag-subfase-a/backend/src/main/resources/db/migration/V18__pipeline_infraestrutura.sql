-- Fase 3: infraestrutura do pipeline documental (auditoria + etapa granular)
-- Não altera o significado de estado (IMPORTADO/PROCESSANDO/PROCESSADO/ERRO).

ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS pipeline_etapa VARCHAR(40),
    ADD COLUMN IF NOT EXISTS pipeline_versao VARCHAR(20);

CREATE TABLE IF NOT EXISTS pipeline_auditoria (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id      UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    etapa             VARCHAR(40) NOT NULL,
    data_inicio       TIMESTAMPTZ NOT NULL,
    data_fim          TIMESTAMPTZ,
    duracao_ms        BIGINT,
    resultado         VARCHAR(20) NOT NULL,
    mensagem_erro     TEXT,
    pipeline_versao   VARCHAR(20) NOT NULL,
    modelo_ia         VARCHAR(120),
    detalhe           TEXT,
    hash_documento    VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_pipeline_auditoria_documento ON pipeline_auditoria(documento_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_auditoria_etapa ON pipeline_auditoria(etapa);
