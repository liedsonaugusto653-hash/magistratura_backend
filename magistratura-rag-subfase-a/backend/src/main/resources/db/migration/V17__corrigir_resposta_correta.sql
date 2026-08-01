ALTER TABLE questoes
ALTER COLUMN resposta_correta TYPE CHAR(1)
USING resposta_correta::CHAR(1);