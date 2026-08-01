-- =====================================================================
-- V12__seeds.sql
-- Utilizador de teste e estatísticas iniciais
-- =====================================================================

-- Utilizador de teste
-- password: 123456

INSERT INTO utilizadores (id, nome, email, password_hash, ativo, data_criacao)
VALUES (
    gen_random_uuid(),
    'Estudante Teste',
    'estudante@magistratura.local',
    '$2a$10$LBslRQCz7kjt4B0OCMwqFOOx6ajJE99gmu.qCuhtMZMW4LgX8EJmC',
    TRUE,
    now()
)
ON CONFLICT (email) DO NOTHING;


-- Estatísticas iniciais do utilizador de teste

INSERT INTO estatisticas (
    utilizador_id,
    horas_estudo,
    dias_consecutivos,
    questoes_respondidas,
    questoes_corretas,
    flashcards_concluidos,
    percentagem_sucesso
)
SELECT 
    id,
    0,
    0,
    0,
    0,
    0,
    0
FROM utilizadores
WHERE email = 'estudante@magistratura.local'
AND NOT EXISTS (
    SELECT 1 
    FROM estatisticas e 
    WHERE e.utilizador_id = utilizadores.id
);