-- Espelha a V9__corrigir_tipo_resposta_estudante.sql: a entidade RespostaSimulado
-- usa @Enumerated(EnumType.STRING) @Column(length = 1), tal como RespostaEstudante,
-- pelo que o Hibernate valida a coluna como CHAR(1). A tabela respostas_simulado
-- ficou como VARCHAR(1) desde a V1__schema.sql original e nunca foi corrigida.
ALTER TABLE respostas_simulado
ALTER COLUMN resposta_escolhida TYPE CHAR(1);