-- Citações RAG usadas pelo Tutor IA: lista JSON de CitacaoFonteResponse
-- (n, artigoId, diplomaId, títulos, extrato, score, …).
-- TEXT (não jsonb) para compatibilidade com bases sem extensão e com o padrão
-- já usado em preferencias_json (V26).

ALTER TABLE mensagens_ia
    ADD COLUMN IF NOT EXISTS fontes_json TEXT;

COMMENT ON COLUMN mensagens_ia.fontes_json IS
    'JSON array de fontes citadas na resposta da IA (marcadores [1],[2],…). Null ou [] se não houve retrieval.';
