-- Corrige divergência entre Entity DocumentoEmbedding e tabela documento_embeddings

ALTER TABLE documento_embeddings
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
