-- Constraints adicionais: unicidade, checks e regras de integridade

ALTER TABLE utilizadores
    ADD CONSTRAINT uq_utilizadores_email UNIQUE (email);

ALTER TABLE categorias
    ADD CONSTRAINT uq_categorias_nome UNIQUE (nome);

ALTER TABLE diplomas
    ADD CONSTRAINT ck_diplomas_estado CHECK (estado IN ('VIGENTE', 'REVOGADO', 'ALTERADO'));

ALTER TABLE questoes
    ADD CONSTRAINT ck_questoes_resposta_correta CHECK (resposta_correta IN ('A', 'B', 'C', 'D')),
    ADD CONSTRAINT ck_questoes_nivel_dificuldade CHECK (nivel_dificuldade IN ('FACIL', 'MEDIO', 'DIFICIL'));

ALTER TABLE respostas_simulado
    ADD CONSTRAINT ck_respostas_simulado_opcao CHECK (resposta_escolhida IN ('A', 'B', 'C', 'D') OR resposta_escolhida IS NULL),
    ADD CONSTRAINT uq_respostas_simulado UNIQUE (tentativa_id, questao_id);

ALTER TABLE simulado_questoes
    ADD CONSTRAINT uq_simulado_questoes UNIQUE (simulado_id, questao_id);

ALTER TABLE mensagens_ia
    ADD CONSTRAINT ck_mensagens_ia_autor CHECK (autor IN ('UTILIZADOR', 'IA'));

ALTER TABLE simulados
    ADD CONSTRAINT ck_simulados_tempo CHECK (tempo_minutos > 0);

ALTER TABLE artigos
    ADD CONSTRAINT ck_artigos_ordem CHECK (ordem >= 0);

ALTER TABLE estatisticas
    ADD CONSTRAINT uq_estatisticas_utilizador UNIQUE (utilizador_id);
