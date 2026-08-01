-- V11__corrigir_tutor_ia.sql
-- Alinha as tabelas do Tutor IA com as Entities Java (idempotente)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'conversas_ia' AND column_name = 'data_atualizacao'
    ) THEN
        ALTER TABLE conversas_ia
            ADD COLUMN data_atualizacao TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'mensagens_ia' AND column_name = 'diploma_contexto_id'
    ) THEN
        ALTER TABLE mensagens_ia
            ADD COLUMN diploma_contexto_id UUID REFERENCES diplomas(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'mensagens_ia' AND column_name = 'artigo_contexto_id'
    ) THEN
        ALTER TABLE mensagens_ia
            ADD COLUMN artigo_contexto_id UUID REFERENCES artigos(id);
    END IF;
END $$;