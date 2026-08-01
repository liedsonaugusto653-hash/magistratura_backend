-- =============================================================================
-- Ontologia Jurídica — Ficha de Estudo por conceito (Fase 1)
-- Adiciona cache de definição + perguntas-guia geradas por IA a cada tópico.
-- Não substitui nada existente: apenas enriquece topicos_juridicos.
-- =============================================================================

ALTER TABLE topicos_juridicos
    ADD COLUMN IF NOT EXISTS definicao_estudo TEXT,
    ADD COLUMN IF NOT EXISTS perguntas_guia TEXT,
    ADD COLUMN IF NOT EXISTS perguntas_guia_gerado_em TIMESTAMPTZ;
