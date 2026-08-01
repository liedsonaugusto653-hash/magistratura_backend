INSERT INTO utilizadores
(id, nome, email, password_hash, ativo)
SELECT
    gen_random_uuid(),
    'Estudante Magistratura',
    'estudante@magistratura.local',
    '$2b$10$PRHbD3wlPGQo/1Sy/VfNTOH0n9YhQIZZkYtFU0v5Bx/T1S.WSCqqK',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM utilizadores
    WHERE email = 'estudante@magistratura.local'
);