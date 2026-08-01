-- Knowledge Domain: origem lógica estável + ciclo de vida dos artefactos

CREATE TABLE IF NOT EXISTS knowledge_origin (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- chave lógica estável (não muda quando o Artigo JPA é recriado no reprocessamento)
    origin_key        VARCHAR(200) NOT NULL UNIQUE,
    documento_id      UUID REFERENCES documentos(id) ON DELETE SET NULL,
    diploma_id        UUID REFERENCES diplomas(id) ON DELETE SET NULL,
    artigo_numero     VARCHAR(30),
    artigo_hash       VARCHAR(64),
    artigo_id_atual   UUID REFERENCES artigos(id) ON DELETE SET NULL,
    pipeline_version  VARCHAR(20),
    data_criacao      TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_atualizacao  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_knowledge_origin_documento ON knowledge_origin(documento_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_origin_diploma ON knowledge_origin(diploma_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_origin_key ON knowledge_origin(origin_key);

-- Referência à origem lógica nos artefactos
ALTER TABLE flashcards
    ADD COLUMN IF NOT EXISTS knowledge_origin_id UUID REFERENCES knowledge_origin(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS generation_status VARCHAR(20) DEFAULT 'PENDENTE';

ALTER TABLE questoes
    ADD COLUMN IF NOT EXISTS knowledge_origin_id UUID REFERENCES knowledge_origin(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS generation_status VARCHAR(20) DEFAULT 'PENDENTE';

-- Métricas agregadas por execução de pipeline (sem dashboard ainda)
CREATE TABLE IF NOT EXISTS pipeline_metricas (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id          UUID REFERENCES documentos(id) ON DELETE CASCADE,
    execucao_id           UUID,
    pipeline_version      VARCHAR(20) NOT NULL,
    data_registo          TIMESTAMPTZ NOT NULL DEFAULT now(),
    documentos_processados INTEGER DEFAULT 1,
    artigos_extraidos     INTEGER DEFAULT 0,
    artigos_novos         INTEGER DEFAULT 0,
    artigos_alterados     INTEGER DEFAULT 0,
    artigos_removidos     INTEGER DEFAULT 0,
    conhecimento_gerado   INTEGER DEFAULT 0,
    falhas                INTEGER DEFAULT 0,
    duracao_total_ms      BIGINT,
    detalhe_etapas        TEXT
);

CREATE INDEX IF NOT EXISTS idx_pipeline_metricas_documento ON pipeline_metricas(documento_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_metricas_data ON pipeline_metricas(data_registo);
