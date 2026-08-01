-- Refinamentos Fase 3: hash por artigo, proveniência de geração, auditoria rica, reprocessamento

-- Hash de conteúdo do artigo (para diff incremental fino)
ALTER TABLE artigos
    ADD COLUMN IF NOT EXISTS hash_conteudo VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_artigos_hash_conteudo ON artigos(hash_conteudo);
CREATE INDEX IF NOT EXISTS idx_artigos_documento_numero ON artigos(documento_id, numero);

-- Proveniência em artefactos gerados (preparação Fase 4 — colunas nullable)
ALTER TABLE flashcards
    ADD COLUMN IF NOT EXISTS artigo_id UUID REFERENCES artigos(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS documento_id UUID REFERENCES documentos(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS pipeline_version VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS ai_model VARCHAR(120),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(40),
    ADD COLUMN IF NOT EXISTS gerado_em TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS estado_validacao VARCHAR(20) DEFAULT 'GERADO';

ALTER TABLE questoes
    ADD COLUMN IF NOT EXISTS artigo_id UUID REFERENCES artigos(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS documento_id UUID REFERENCES documentos(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS pipeline_version VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS ai_model VARCHAR(120),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(40),
    ADD COLUMN IF NOT EXISTS gerado_em TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS estado_validacao VARCHAR(20) DEFAULT 'GERADO';

-- Auditoria alargada
ALTER TABLE pipeline_auditoria
    ADD COLUMN IF NOT EXISTS artigos_extraidos INTEGER,
    ADD COLUMN IF NOT EXISTS num_erros INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS num_avisos INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stacktrace_resumo TEXT,
    ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(40);

-- Última etapa concluída com sucesso (reexecução parcial)
ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS ultima_etapa_ok VARCHAR(40);
