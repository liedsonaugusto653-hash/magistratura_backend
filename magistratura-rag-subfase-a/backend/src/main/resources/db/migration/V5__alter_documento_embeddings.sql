ALTER TABLE documento_embeddings
ADD COLUMN conteudo TEXT;

ALTER TABLE documento_embeddings
RENAME COLUMN modelo TO modelo_embedding;

ALTER TABLE documento_embeddings
ADD COLUMN origem VARCHAR(200);

ALTER TABLE documento_embeddings
ADD COLUMN data_criacao TIMESTAMP NOT NULL DEFAULT NOW();