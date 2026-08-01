# Pipeline de Ingestão Jurídica 2.0

## Princípio

```
OCR / PDFBox  →  extracção determinística  →  artigos  →  embeddings  →  IA responde
```

A **extracção de artigos é determinística** (regex + estrutura).  
Não há modelo generativo a inventar números de artigos.

## Fluxo

```
PDF
 │
 ├─ DocumentValidatorStage
 ├─ PdfAnalysisStage          → TEXT | IMAGE | HYBRID | PROTECTED
 ├─ PdfTextExtractorStage
 │     ├─ PDFBox (nativo)
 │     ├─ se chars < limiar → OCR Tesseract (se enabled)
 │     └─ TextoJuridicoNormalizer
 ├─ MetadataExtractorStage
 ├─ StructureExtractorStage   → EstruturaJuridicaParser
 ├─ ArticlePersistenceStage
 ├─ KnowledgeIndexerStage     → RAG / Knowledge Layer
 └─ KnowledgeGeneratorStage   → opcional (desligado por defeito)
```

## Estados do documento

| Estado | Significado |
|--------|-------------|
| IMPORTADO | PDF guardado, ainda não processado |
| PROCESSANDO / EXTRAINDO_TEXTO / OCR_EM_EXECUCAO / ESTRUTURANDO | Em curso |
| PROCESSADO | Artigos extraídos, qualidade OK |
| PROCESSADO_COM_AVISOS | Concluído com OCR, baixa confiança ou 0 artigos estruturados |
| FALHA_EXTRACAO | Sem texto útil (scan sem OCR, PDF bloqueado) |
| ERRO | Falha inesperada |

**Nunca** se marca `PROCESSADO` limpo com 0 artigos.

## Configuração

```yaml
app:
  pipeline:
    versao: 2.0.0
    ocr:
      enabled: true
      language: por
      datapath: ""          # tessdata; vazio = default sistema
      dpi: 200
      max-pages: 0          # 0 = todas
      min-chars-uteis: 40
```

### Dependências de sistema (OCR)

```bash
# Debian/Ubuntu
sudo apt-get install -y tesseract-ocr tesseract-ocr-por

# Verificar
tesseract --list-langs   # deve incluir 'por'
```

Se o OCR estiver indisponível e o PDF for só-imagem, o pipeline **falha com mensagem clara** (`FALHA_EXTRACAO`), em vez de “0 artigos”.

## Maven

- `org.apache.pdfbox:pdfbox:3.0.3`
- `org.apache.pdfbox:pdfbox-tools:3.0.3` (renderização de páginas)
- `net.sourceforge.tess4j:tess4j:5.11.0`

## Migration

`V24__pipeline_ingestao_robusta.sql` — colunas `metodo_extracao`, `confianca_extracao`, `tipo_pdf`; `estado` VARCHAR(30).

## Testes manuais

1. **PDF digital (texto)**  
   Importar + processar Constituição/texto seleccionável → `PROCESSADO` ou `PROCESSADO_COM_AVISOS`, `artigos > 0`, `metodo_extracao=PDFBOX`.

2. **PDF imagem** (com Tesseract instalado)  
   Scan → logs “A activar OCR” → artigos se o OCR legível; senão `FALHA_EXTRACAO`.

3. **PDF protegido / 0 chars sem OCR**  
   → `FALHA_EXTRACAO` + mensagem a explicar OCR/texto.

4. **Reprocessar**  
   `POST /api/documentos/{id}/reprocessar` limpa artigos e corre o pipeline 2.0 de novo.

## Frontend

`DocumentoResponse.estado` continua a ser `String`. Valores novos são transparentes; UI pode tratar `PROCESSADO_COM_AVISOS` como sucesso com badge de aviso e `FALHA_EXTRACAO` como erro.
