-- Perfil / preferências do estudante
ALTER TABLE utilizadores
    ADD COLUMN IF NOT EXISTS preferencias_json TEXT;

-- Progresso de processamento visível ao utilizador (OCR / pipeline)
ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS progresso_paginas_ok INTEGER,
    ADD COLUMN IF NOT EXISTS progresso_paginas_total INTEGER,
    ADD COLUMN IF NOT EXISTS progresso_percentagem INTEGER,
    ADD COLUMN IF NOT EXISTS mensagem_progresso VARCHAR(200);

-- Estados longos já usados pelo pipeline 2.0
ALTER TABLE documentos ALTER COLUMN estado TYPE VARCHAR(30);
