-- =============================================================================
-- Ontologia Jurídica — Ficha de Estudo pedagógica (Fase 1b)
-- Amplia a Ficha de Estudo (V30) com o "porquê", exemplos do quotidiano,
-- erros comuns e um caso prático socrático — em vez de só definição + perguntas.
-- =============================================================================

ALTER TABLE topicos_juridicos
    ADD COLUMN IF NOT EXISTS porque_existe TEXT,
    ADD COLUMN IF NOT EXISTS onde_aparece_vida TEXT,
    ADD COLUMN IF NOT EXISTS erros_comuns TEXT,
    ADD COLUMN IF NOT EXISTS caso_pratico TEXT;
