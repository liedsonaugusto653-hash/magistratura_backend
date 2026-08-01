-- Sistema Magistratura - Schema inicial
-- Extensão necessária para gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE utilizadores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(180) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    fotografia_url  TEXT,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT now(),
    ultimo_login    TIMESTAMPTZ
);

CREATE TABLE categorias (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(120) NOT NULL,
    descricao   TEXT
);

CREATE TABLE temas (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    descricao     TEXT,
    categoria_id  UUID REFERENCES categorias(id)
);

CREATE TABLE diplomas (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero             VARCHAR(80) NOT NULL,
    titulo             VARCHAR(300) NOT NULL,
    descricao          TEXT,
    data_publicacao    DATE,
    categoria_id       UUID REFERENCES categorias(id),
    estado             VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    resumo             TEXT,
    pdf_url            TEXT,
    versao             INTEGER NOT NULL DEFAULT 1,
    data_criacao       TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_modificacao   TIMESTAMPTZ
);

CREATE TABLE artigos (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    diploma_id     UUID NOT NULL REFERENCES diplomas(id) ON DELETE CASCADE,
    numero         VARCHAR(30) NOT NULL,
    titulo         VARCHAR(300),
    texto          TEXT NOT NULL,
    ordem          INTEGER NOT NULL,
    resumo         TEXT,
    tema_id        UUID REFERENCES temas(id),
    pagina_inicio  INTEGER,
    pagina_fim     INTEGER
);

CREATE TABLE resumos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo      VARCHAR(250) NOT NULL,
    conteudo    TEXT NOT NULL,
    diploma_id  UUID REFERENCES diplomas(id) ON DELETE CASCADE,
    tema_id     UUID REFERENCES temas(id)
);

CREATE TABLE documento_embeddings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artigo_id  UUID REFERENCES artigos(id) ON DELETE CASCADE,
    resumo_id  UUID REFERENCES resumos(id) ON DELETE CASCADE,
    vetor      DOUBLE PRECISION[],
    modelo     VARCHAR(100)
);


CREATE TABLE flashcards (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pergunta      TEXT NOT NULL,
    resposta      TEXT NOT NULL,
    tema_id       UUID REFERENCES temas(id),
    categoria_id  UUID REFERENCES categorias(id),
    diploma_id    UUID REFERENCES diplomas(id)
);

CREATE TABLE questoes (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enunciado          TEXT NOT NULL,
    opcao_a            TEXT NOT NULL,
    opcao_b            TEXT NOT NULL,
    opcao_c            TEXT NOT NULL,
    opcao_d            TEXT NOT NULL,
    resposta_correta   VARCHAR(1) NOT NULL,
    justificacao       TEXT,
    tema_id            UUID REFERENCES temas(id),
    categoria_id       UUID REFERENCES categorias(id),
    diploma_id         UUID REFERENCES diplomas(id),
    artigo_id          UUID REFERENCES artigos(id),
    nivel_dificuldade  VARCHAR(10) NOT NULL DEFAULT 'MEDIO'
);

CREATE TABLE simulados (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo          VARCHAR(250) NOT NULL,
    descricao       TEXT,
    tempo_minutos   INTEGER NOT NULL,
    categoria_id    UUID REFERENCES categorias(id),
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE simulado_questoes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    simulado_id  UUID NOT NULL REFERENCES simulados(id) ON DELETE CASCADE,
    questao_id   UUID NOT NULL REFERENCES questoes(id) ON DELETE CASCADE,
    ordem        INTEGER NOT NULL
);

CREATE TABLE tentativas_simulado (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    simulado_id  UUID NOT NULL REFERENCES simulados(id) ON DELETE CASCADE,
    data_inicio  TIMESTAMPTZ NOT NULL,
    data_fim     TIMESTAMPTZ,
    pontuacao    DOUBLE PRECISION,
    concluido    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE respostas_simulado (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tentativa_id        UUID NOT NULL REFERENCES tentativas_simulado(id) ON DELETE CASCADE,
    questao_id          UUID NOT NULL REFERENCES questoes(id) ON DELETE CASCADE,
    resposta_escolhida  VARCHAR(1),
    correta             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE estatisticas (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id         UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    horas_estudo          DOUBLE PRECISION NOT NULL DEFAULT 0,
    dias_consecutivos     INTEGER NOT NULL DEFAULT 0,
    questoes_respondidas  INTEGER NOT NULL DEFAULT 0,
    questoes_corretas     INTEGER NOT NULL DEFAULT 0,
    flashcards_concluidos INTEGER NOT NULL DEFAULT 0,
    percentagem_sucesso   DOUBLE PRECISION NOT NULL DEFAULT 0,
    ultima_atividade      TIMESTAMPTZ
);

CREATE TABLE historico_estudo (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id  UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    artigo_id      UUID REFERENCES artigos(id) ON DELETE CASCADE,
    diploma_id     UUID REFERENCES diplomas(id) ON DELETE CASCADE,
    tempo_segundos INTEGER NOT NULL,
    data           TIMESTAMPTZ NOT NULL,
    ultima_pagina  INTEGER
);

CREATE TABLE favoritos (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id  UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    artigo_id      UUID REFERENCES artigos(id) ON DELETE CASCADE,
    diploma_id     UUID REFERENCES diplomas(id) ON DELETE CASCADE,
    data_criacao   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversas_ia (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id  UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    titulo         VARCHAR(250) NOT NULL,
    data_criacao   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE mensagens_ia (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversa_id  UUID NOT NULL REFERENCES conversas_ia(id) ON DELETE CASCADE,
    autor        VARCHAR(15) NOT NULL,
    conteudo     TEXT NOT NULL,
    timestamp    TIMESTAMPTZ NOT NULL
);