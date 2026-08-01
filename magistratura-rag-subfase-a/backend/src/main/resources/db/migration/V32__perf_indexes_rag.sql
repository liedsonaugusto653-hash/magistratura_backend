-- Parte 2 auditoria: índices de suporte a RAG, ontologia e listagens quentes.
-- IF NOT EXISTS para ser idempotente em ambientes que já criaram alguns.

CREATE INDEX IF NOT EXISTS idx_artigos_diploma_numero ON artigos (diploma_id, lower(numero));
CREATE INDEX IF NOT EXISTS idx_artigos_documento ON artigos (documento_id);

CREATE INDEX IF NOT EXISTS idx_documentos_estado ON documentos (estado);
CREATE INDEX IF NOT EXISTS idx_documentos_diploma ON documentos (diploma_id);
CREATE INDEX IF NOT EXISTS idx_documentos_categoria ON documentos (categoria_id);

CREATE INDEX IF NOT EXISTS idx_mensagens_ia_conversa_ts ON mensagens_ia (conversa_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_topicos_juridicos_entidade ON topicos_juridicos (entidade_id);
CREATE INDEX IF NOT EXISTS idx_topicos_juridicos_codigo ON topicos_juridicos (codigo);

-- Coluna real: tipo_relacao (não "tipo") — ver entity RelacaoJuridica / V28
CREATE INDEX IF NOT EXISTS idx_relacoes_tipo ON relacoes_juridicas (tipo_relacao);

-- FTS auxiliar em título de artigo (quando preenchido)
CREATE INDEX IF NOT EXISTS idx_artigos_titulo_fts
    ON artigos USING gin (to_tsvector('portuguese', coalesce(titulo, '')));