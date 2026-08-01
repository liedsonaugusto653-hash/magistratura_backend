# OCR reforçado para PDFs legislativos (scans / só-imagem)

## Problema

A maior parte dos PDFs oficiais (Diário da República, decretos antigos, acórdãos)
são **imagens digitalizadas**, sem camada de texto. O PDFBox sozinho devolve 0 chars.

## Solução nesta entrega

1. **Pré-processamento de imagem** (`ImageOcrPreprocessor`)
   - Upscale 2× se largura < 1200 px
   - Conversão a cinzento + reforço de contraste
   - 2.ª tentativa com **binarização Otsu** se a 1.ª devolver pouco texto

2. **OCR Tesseract melhorado** (`OcrExtractorService`)
   - DPI por omissão **250** (antes 200)
   - PSM 6 (bloco de texto), OEM LSTM
   - `preserve_interword_spaces`
   - OCR **selectivo** de páginas (só as que não têm texto nativo)

3. **Pipeline híbrido** (`PdfTextExtractorStage`)
   - PDF 100% imagem → OCR total
   - PDF híbrido (algumas páginas com texto) → OCR só nas páginas fracas
   - Progresso SSE: «OCR: página X de Y…»

## Configuração (`application.yml` / env)

| Variável | Default | Notas |
|----------|---------|--------|
| `PIPELINE_OCR_ENABLED` | true | |
| `PIPELINE_OCR_LANGUAGE` | por | `por+eng` se necessário |
| `PIPELINE_OCR_DATAPATH` | *(vazio = auto)* | Windows: `C:/Program Files/Tesseract-OCR/tessdata` |
| `PIPELINE_OCR_DPI` | 250 | 300 em scans muito finos |
| `PIPELINE_OCR_PREPROCESS` | true | |
| `PIPELINE_OCR_PSM` | 6 | |
| `PIPELINE_OCR_MIN_CHARS` | 40 | mínimo no documento |
| `PIPELINE_OCR_MIN_CHARS_PAGE` | 15 | página candidata a OCR |

## Dependências de sistema

```bash
# Debian/Ubuntu
sudo apt-get install -y tesseract-ocr tesseract-ocr-por
tesseract --list-langs   # deve listar 'por'

# Windows
# Instalar Tesseract OCR e opcionalmente:
# set PIPELINE_OCR_DATAPATH=C:\Program Files\Tesseract-OCR\tessdata
```

## Ficheiros

- `service/pdf/ImageOcrPreprocessor.java` (**novo**)
- `service/pdf/OcrExtractorService.java`
- `pipeline/stage/PdfTextExtractorStage.java`
- `resources/application.yml`

## Validação

1. Importar PDF scan da Constituição / Diário → processar
2. Log: `A activar OCR`, `OCR progresso`, `método=OCR_TESSERACT` ou `HIBRIDO`
3. Estado final `PROCESSADO` ou `PROCESSADO_COM_AVISOS` com artigos > 0
4. Sem Tesseract instalado → mensagem clara `FALHA_EXTRACAO` (não fica a 0% eterno)
