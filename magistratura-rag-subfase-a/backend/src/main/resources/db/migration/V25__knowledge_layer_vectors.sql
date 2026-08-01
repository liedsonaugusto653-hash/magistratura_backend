-- V25: idempotente. Se V21 já criou knowledge_vectors, não faz nada destrutivo.
-- Tenta pgvector; se já existir tabela, só garante índices básicos.

DO $ext$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgvector indisponível em V25: %', SQLERRM;
END
$ext$;

-- Só cria se ainda não existir (bases que saltaram V21 antigo)
DO $body$
BEGIN
    IF to_regclass('public.knowledge_vectors') IS NOT NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'knowledge_vectors') THEN
        IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
            EXECUTE $v$
                CREATE TABLE knowledge_vectors (
                    id UUID PRIMARY KEY,
                    artigo_id UUID REFERENCES artigos(id) ON DELETE CASCADE,
                    diploma_id UUID REFERENCES diplomas(id) ON DELETE SET NULL,
                    documento_id UUID REFERENCES documentos(id) ON DELETE CASCADE,
                    kind VARCHAR(40) NOT NULL DEFAULT 'LEGISLACAO',
                    texto TEXT NOT NULL,
                    embedding vector(768),
                    modelo_embedding VARCHAR(100) NOT NULL,
                    metadados JSONB NOT NULL DEFAULT '{}',
                    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
                    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
                )
            $v$;
        ELSE
            EXECUTE $v$
                CREATE TABLE knowledge_vectors (
                    id UUID PRIMARY KEY,
                    artigo_id UUID REFERENCES artigos(id) ON DELETE CASCADE,
                    diploma_id UUID REFERENCES diplomas(id) ON DELETE SET NULL,
                    documento_id UUID REFERENCES documentos(id) ON DELETE CASCADE,
                    kind VARCHAR(40) NOT NULL DEFAULT 'LEGISLACAO',
                    texto TEXT NOT NULL,
                    embedding REAL[],
                    modelo_embedding VARCHAR(100) NOT NULL,
                    metadados JSONB NOT NULL DEFAULT '{}',
                    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
                    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
                )
            $v$;
        END IF;
    END IF;
END
$body$;

CREATE INDEX IF NOT EXISTS idx_kv_diploma ON knowledge_vectors (diploma_id);
CREATE INDEX IF NOT EXISTS idx_kv_artigo ON knowledge_vectors (artigo_id);
CREATE INDEX IF NOT EXISTS idx_kv_documento ON knowledge_vectors (documento_id);
