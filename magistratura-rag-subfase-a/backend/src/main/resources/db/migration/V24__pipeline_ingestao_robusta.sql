-- Pipeline 2.0: ingestão jurídica robusta (análise PDF, OCR, qualidade)

-- Estados novos podem ultrapassar 20 chars se no futuro; alarga preventivamente
ALTER TABLE documentos
    ALTER COLUMN estado TYPE VARCHAR(30);

ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS metodo_extracao VARCHAR(30),
    ADD COLUMN IF NOT EXISTS confianca_extracao INTEGER,
    ADD COLUMN IF NOT EXISTS tipo_pdf VARCHAR(20);

COMMENT ON COLUMN documentos.metodo_extracao IS 'PDFBOX | OCR_TESSERACT | HIBRIDO | NENHUM';
COMMENT ON COLUMN documentos.confianca_extracao IS 'Heurística 0-100 da qualidade da extracção';
COMMENT ON COLUMN documentos.tipo_pdf IS 'TEXT | IMAGE | HYBRID | PROTECTED | UNKNOWN';
