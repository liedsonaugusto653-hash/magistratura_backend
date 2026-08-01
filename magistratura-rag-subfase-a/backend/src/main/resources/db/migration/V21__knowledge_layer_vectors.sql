-- Knowledge Layer vector store.
-- Com pgvector: coluna vector(768) + índice HNSW.
-- Sem pgvector (Postgres stock): REAL[] — app.knowledge.vector-store=noop continua a funcionar.
-- Não falha o arranque se a extensão não estiver instalada no sistema.

DO $ext$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgvector indisponível (%). Schema fallback sem tipo vector.', SQLERRM;
END
$ext$;

DO $body$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        EXECUTE $v$
            CREATE TABLE IF NOT EXISTS knowledge_vectors (
                id                UUID PRIMARY KEY,
                artigo_id         UUID REFERENCES artigos(id) ON DELETE CASCADE,
                diploma_id        UUID REFERENCES diplomas(id) ON DELETE SET NULL,
                documento_id      UUID REFERENCES documentos(id) ON DELETE CASCADE,
                kind              VARCHAR(40) NOT NULL DEFAULT 'LEGISLACAO',
                texto             TEXT NOT NULL,
                embedding         vector(768),
                modelo_embedding  VARCHAR(100) NOT NULL,
                metadados         JSONB NOT NULL DEFAULT '{}',
                criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
                atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
            )
        $v$;
        EXECUTE $v$
            CREATE INDEX IF NOT EXISTS idx_kv_embedding_hnsw
                ON knowledge_vectors
                USING hnsw (embedding vector_cosine_ops)
                WITH (m = 16, ef_construction = 64)
        $v$;
        EXECUTE $v$
            CREATE TABLE IF NOT EXISTS embedding_cache (
                hash_texto        VARCHAR(64) NOT NULL,
                modelo_embedding  VARCHAR(100) NOT NULL,
                embedding         vector(768) NOT NULL,
                criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
                PRIMARY KEY (hash_texto, modelo_embedding)
            )
        $v$;
    ELSE
        EXECUTE $v$
            CREATE TABLE IF NOT EXISTS knowledge_vectors (
                id                UUID PRIMARY KEY,
                artigo_id         UUID REFERENCES artigos(id) ON DELETE CASCADE,
                diploma_id        UUID REFERENCES diplomas(id) ON DELETE SET NULL,
                documento_id      UUID REFERENCES documentos(id) ON DELETE CASCADE,
                kind              VARCHAR(40) NOT NULL DEFAULT 'LEGISLACAO',
                texto             TEXT NOT NULL,
                embedding         REAL[],
                modelo_embedding  VARCHAR(100) NOT NULL,
                metadados         JSONB NOT NULL DEFAULT '{}',
                criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
                atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
            )
        $v$;
        EXECUTE $v$
            CREATE TABLE IF NOT EXISTS embedding_cache (
                hash_texto        VARCHAR(64) NOT NULL,
                modelo_embedding  VARCHAR(100) NOT NULL,
                embedding         REAL[] NOT NULL,
                criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
                PRIMARY KEY (hash_texto, modelo_embedding)
            )
        $v$;
    END IF;
END
$body$;

CREATE INDEX IF NOT EXISTS idx_kv_diploma ON knowledge_vectors (diploma_id);
CREATE INDEX IF NOT EXISTS idx_kv_artigo ON knowledge_vectors (artigo_id);
CREATE INDEX IF NOT EXISTS idx_kv_documento ON knowledge_vectors (documento_id);
CREATE INDEX IF NOT EXISTS idx_kv_modelo ON knowledge_vectors (modelo_embedding);
CREATE INDEX IF NOT EXISTS idx_kv_kind ON knowledge_vectors (kind);

COMMENT ON TABLE knowledge_vectors IS 'Chunks RAG Knowledge Layer (separado de documento_embeddings legado)';
