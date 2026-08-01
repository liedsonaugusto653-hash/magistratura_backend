-- =============================================================================
-- Ontologia Jurídica — Camada conceptual (Fase 1)
-- Separa o conhecimento por ENTIDADES e TÓPICOS, sem substituir diplomas/artigos.
-- =============================================================================

-- Entidades de primeiro nível (Pessoa, Estado, Contrato, Crime, …)
CREATE TABLE IF NOT EXISTS entidades_juridicas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo          VARCHAR(80)  NOT NULL UNIQUE,   -- ex.: ESTADO, PESSOA, CONTRATO
    nome            VARCHAR(150) NOT NULL,
    descricao       TEXT,
    icone           VARCHAR(40),                    -- opcional (UI)
    ordem           INT NOT NULL DEFAULT 0,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tópicos hierárquicos sob uma entidade (ou raiz)
CREATE TABLE IF NOT EXISTS topicos_juridicos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo          VARCHAR(120) NOT NULL UNIQUE,  -- ex.: ESTADO.PODER_JUDICIAL
    nome            VARCHAR(200) NOT NULL,
    descricao       TEXT,
    entidade_id     UUID REFERENCES entidades_juridicas(id) ON DELETE SET NULL,
    parent_id       UUID REFERENCES topicos_juridicos(id) ON DELETE SET NULL,
    categoria_id    UUID REFERENCES categorias(id) ON DELETE SET NULL, -- ponte opcional ao legado
    ordem           INT NOT NULL DEFAULT 0,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_topicos_entidade ON topicos_juridicos(entidade_id);
CREATE INDEX IF NOT EXISTS idx_topicos_parent   ON topicos_juridicos(parent_id);

-- Relações tipadas entre tópicos (grafo)
-- tipos: REGULADO_POR | PRESSUPOE | OPÕE_SE | ESPECIALIZA | APLICA_SE_A | CONEXO
CREATE TABLE IF NOT EXISTS relacoes_juridicas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origem_id       UUID NOT NULL REFERENCES topicos_juridicos(id) ON DELETE CASCADE,
    destino_id      UUID NOT NULL REFERENCES topicos_juridicos(id) ON DELETE CASCADE,
    tipo_relacao    VARCHAR(40) NOT NULL,
    peso            REAL NOT NULL DEFAULT 1.0,
    notas           TEXT,
    CONSTRAINT uq_relacao UNIQUE (origem_id, destino_id, tipo_relacao),
    CONSTRAINT chk_relacao_nao_reflexiva CHECK (origem_id <> destino_id)
);

CREATE INDEX IF NOT EXISTS idx_relacoes_origem  ON relacoes_juridicas(origem_id);
CREATE INDEX IF NOT EXISTS idx_relacoes_destino ON relacoes_juridicas(destino_id);

-- Ligação tópico ↔ artigo (N:N) — o diploma fica implícito via artigo
CREATE TABLE IF NOT EXISTS topico_artigo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topico_id       UUID NOT NULL REFERENCES topicos_juridicos(id) ON DELETE CASCADE,
    artigo_id       UUID NOT NULL REFERENCES artigos(id) ON DELETE CASCADE,
    relevancia      REAL NOT NULL DEFAULT 1.0,     -- 0..1 ranking manual/automático
    origem_ligacao  VARCHAR(30) NOT NULL DEFAULT 'MANUAL', -- MANUAL | IA | IMPORT
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_topico_artigo UNIQUE (topico_id, artigo_id)
);

CREATE INDEX IF NOT EXISTS idx_topico_artigo_topico ON topico_artigo(topico_id);
CREATE INDEX IF NOT EXISTS idx_topico_artigo_artigo ON topico_artigo(artigo_id);

-- =============================================================================
-- Seed mínimo — entidades conceptuais (Angola / estudo magistratura)
-- =============================================================================
INSERT INTO entidades_juridicas (codigo, nome, descricao, ordem) VALUES
    ('ESTADO',               'Estado',                'Organização política, soberania, poderes e administração pública', 10),
    ('PESSOA',               'Pessoa',                'Personalidade, capacidade, direitos e deveres fundamentais', 20),
    ('FAMILIA',              'Família',               'Casamento, filiação, adoção, união de facto', 30),
    ('PROPRIEDADE',          'Propriedade',           'Direitos reais, posse, propriedade e registo', 40),
    ('CONTRATO',             'Contrato',              'Negócio jurídico e contratos em especial', 50),
    ('RESPONSABILIDADE',     'Responsabilidade Civil','Dano, culpa, nexo causal e indemnização', 60),
    ('TRABALHO',             'Trabalho',              'Relação laboral, empregador, trabalhador e segurança social', 70),
    ('CRIME',                'Crime',                 'Ilícito penal, tipicidade, ilicitude e culpabilidade', 80),
    ('PROCESSO_CIVIL',       'Processo Civil',        'Acção, competência, provas, recursos e execução', 90),
    ('PROCESSO_PENAL',       'Processo Penal',        'Inquérito, instrução, julgamento e medidas de coacção', 100),
    ('ADMINISTRACAO',        'Administração Pública', 'Acto administrativo, procedimento e contencioso', 110),
    ('TRIBUNAL',             'Tribunal',              'Organização judiciária, juízes e competência', 120)
ON CONFLICT (codigo) DO NOTHING;

-- Tópicos de 1.º nível sob cada entidade (amostra útil para estudo)
INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'ESTADO.PODERES', 'Poderes do Estado', 'Legislativo, Executivo e Judicial', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'ESTADO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'ESTADO.PODER_JUDICIAL', 'Poder Judicial', 'Independência e estrutura dos tribunais', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'ESTADO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'ESTADO.RESPONSABILIDADE', 'Responsabilidade do Estado', 'Responsabilidade civil extracontratual da Administração', e.id, 3
FROM entidades_juridicas e WHERE e.codigo = 'ESTADO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PESSOA.PERSONALIDADE', 'Personalidade jurídica', 'Início e termo da personalidade', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'PESSOA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PESSOA.CAPACIDADE', 'Capacidade jurídica', 'Capacidade de gozo e de exercício', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'PESSOA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PESSOA.DIREITOS_FUNDAMENTAIS', 'Direitos fundamentais', 'Direitos, liberdades e garantias', e.id, 3
FROM entidades_juridicas e WHERE e.codigo = 'PESSOA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PESSOA.NACIONALIDADE', 'Nacionalidade', 'Aquisição, perda e reaquisição da nacionalidade', e.id, 4
FROM entidades_juridicas e WHERE e.codigo = 'PESSOA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'FAMILIA.CASAMENTO', 'Casamento', 'Requisitos, efeitos e regimes de bens', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'FAMILIA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'FAMILIA.DIVORCIO', 'Divórcio', 'Modalidades e efeitos do divórcio', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'FAMILIA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'FAMILIA.FILIACAO', 'Filiação', 'Estabelecimento e efeitos da filiação', e.id, 3
FROM entidades_juridicas e WHERE e.codigo = 'FAMILIA'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'CONTRATO.FORMACAO', 'Formação do contrato', 'Proposta, aceitação e forma', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'CONTRATO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'CONTRATO.COMPRA_VENDA', 'Compra e venda', 'Contrato de compra e venda', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'CONTRATO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'CONTRATO.ARRENDAMENTO', 'Arrendamento', 'Arrendamento urbano e rústico', e.id, 3
FROM entidades_juridicas e WHERE e.codigo = 'CONTRATO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'RESPONSABILIDADE.PRESSUPOSTOS', 'Pressupostos da responsabilidade', 'Dano, culpa, ilicitude e nexo causal', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'RESPONSABILIDADE'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'RESPONSABILIDADE.INDEMNIZACAO', 'Indemnização', 'Cálculo e modalidades de indemnização', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'RESPONSABILIDADE'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'TRABALHO.EMPREGADOR', 'Empregador', 'Direitos e deveres do empregador', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'TRABALHO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'TRABALHO.TRABALHADOR', 'Trabalhador', 'Direitos e deveres do trabalhador', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'TRABALHO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'TRABALHO.CONTRATO_TRABALHO', 'Contrato de trabalho', 'Formação, tipos e cessação', e.id, 3
FROM entidades_juridicas e WHERE e.codigo = 'TRABALHO'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'CRIME.TIPICIDADE', 'Tipicidade', 'Elementos do tipo legal de crime', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'CRIME'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'CRIME.CULPABILIDADE', 'Culpabilidade', 'Imputabilidade e dolo/negligência', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'CRIME'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PROCESSO_PENAL.PRISAO_PREVENTIVA', 'Prisão preventiva', 'Pressupostos e prazos da prisão preventiva', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'PROCESSO_PENAL'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PROCESSO_PENAL.PRESUNCAO_INOCENCIA', 'Presunção de inocência', 'Garantia processual fundamental', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'PROCESSO_PENAL'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PROCESSO_CIVIL.COMPETENCIA', 'Competência', 'Competência material, territorial e funcional', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'PROCESSO_CIVIL'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'PROCESSO_CIVIL.RECURSOS', 'Recursos', 'Apelação, revista e outros meios de impugnação', e.id, 2
FROM entidades_juridicas e WHERE e.codigo = 'PROCESSO_CIVIL'
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO topicos_juridicos (codigo, nome, descricao, entidade_id, ordem)
SELECT 'TRIBUNAL.ORGANIZACAO', 'Organização judiciária', 'Hierarquia e tipos de tribunais', e.id, 1
FROM entidades_juridicas e WHERE e.codigo = 'TRIBUNAL'
ON CONFLICT (codigo) DO NOTHING;

-- Relações conceptuais de exemplo
INSERT INTO relacoes_juridicas (origem_id, destino_id, tipo_relacao, peso, notas)
SELECT o.id, d.id, 'PRESSUPOE', 1.0, 'Responsabilidade civil do Estado pressupõe personalidade/capacidade do lesado'
FROM topicos_juridicos o, topicos_juridicos d
WHERE o.codigo = 'ESTADO.RESPONSABILIDADE' AND d.codigo = 'PESSOA.PERSONALIDADE'
ON CONFLICT (origem_id, destino_id, tipo_relacao) DO NOTHING;

INSERT INTO relacoes_juridicas (origem_id, destino_id, tipo_relacao, peso, notas)
SELECT o.id, d.id, 'CONEXO', 0.9, 'Poder Judicial articula-se com organização dos tribunais'
FROM topicos_juridicos o, topicos_juridicos d
WHERE o.codigo = 'ESTADO.PODER_JUDICIAL' AND d.codigo = 'TRIBUNAL.ORGANIZACAO'
ON CONFLICT (origem_id, destino_id, tipo_relacao) DO NOTHING;

INSERT INTO relacoes_juridicas (origem_id, destino_id, tipo_relacao, peso, notas)
SELECT o.id, d.id, 'ESPECIALIZA', 1.0, 'Compra e venda é espécie de contrato'
FROM topicos_juridicos o, topicos_juridicos d
WHERE o.codigo = 'CONTRATO.COMPRA_VENDA' AND d.codigo = 'CONTRATO.FORMACAO'
ON CONFLICT (origem_id, destino_id, tipo_relacao) DO NOTHING;

INSERT INTO relacoes_juridicas (origem_id, destino_id, tipo_relacao, peso, notas)
SELECT o.id, d.id, 'PRESSUPOE', 1.0, 'Indemnização pressupõe os pressupostos da responsabilidade'
FROM topicos_juridicos o, topicos_juridicos d
WHERE o.codigo = 'RESPONSABILIDADE.INDEMNIZACAO' AND d.codigo = 'RESPONSABILIDADE.PRESSUPOSTOS'
ON CONFLICT (origem_id, destino_id, tipo_relacao) DO NOTHING;
