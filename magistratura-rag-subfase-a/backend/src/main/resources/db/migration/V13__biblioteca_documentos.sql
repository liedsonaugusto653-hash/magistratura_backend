-- Módulo de Biblioteca Jurídica (PDFs)
--
-- Um Diploma pode ter vários Documentos (PDFs) associados: a versão original,
-- uma versão consolidada, uma edição comentada, uma correção, etc.
-- diploma_id é NULL enquanto o documento está a ser importado/processado;
-- torna-se obrigatório (em código, não aqui) assim que o estado passa a PROCESSADO.

CREATE TABLE documentos (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo                   VARCHAR(300) NOT NULL,
    categoria_id             UUID REFERENCES categorias(id),
    diploma_id               UUID REFERENCES diplomas(id) ON DELETE SET NULL,
    versao                   INTEGER NOT NULL DEFAULT 1,
    estado                   VARCHAR(20) NOT NULL DEFAULT 'IMPORTADO',
    fonte                    VARCHAR(120),
    oficial                  BOOLEAN NOT NULL DEFAULT TRUE,
    data_publicacao          DATE,
    data_importacao          TIMESTAMPTZ NOT NULL DEFAULT now(),
    hash_ficheiro            VARCHAR(64) NOT NULL UNIQUE,
    numero_paginas           INTEGER,
    caminho_ficheiro         VARCHAR(500) NOT NULL,
    tamanho_bytes            BIGINT,
    revisao_necessaria       BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes_processamento TEXT,
    data_modificacao         TIMESTAMPTZ
);

CREATE INDEX idx_documentos_diploma ON documentos(diploma_id);
CREATE INDEX idx_documentos_categoria ON documentos(categoria_id);
CREATE INDEX idx_documentos_estado ON documentos(estado);

-- Cada artigo passa a saber de que documento (PDF) foi extraído.
-- Nullable porque artigos podem continuar a existir sem origem documental
-- (ex.: os que já estavam na base de dados antes deste módulo).
ALTER TABLE artigos ADD COLUMN documento_id UUID REFERENCES documentos(id) ON DELETE SET NULL;
ALTER TABLE artigos ADD COLUMN capitulo VARCHAR(200);
ALTER TABLE artigos ADD COLUMN seccao VARCHAR(200);

CREATE INDEX idx_artigos_documento ON artigos(documento_id);

-- pdf_url deixa de ser necessário em diplomas: o ficheiro passa a viver em Documento.
-- Mantemos a coluna por agora (não eliminamos dados existentes sem necessidade
-- comprovada), apenas deixamos de a preencher a partir daqui.
COMMENT ON COLUMN diplomas.pdf_url IS 'Obsoleto: o PDF passa a ser gerido via tabela documentos. Mantido por compatibilidade.';
