# Magistratura — patches da sessão (citações, SSE, OCR, pré-texto, ordem)

**Data:** 2026-07-29  
Sobrepor estes ficheiros na raiz do projecto original (`magistratura-rag-subfase-a/` ou monorepo equivalente).

## Conteúdo

| Área | O que faz |
|------|-----------|
| **Citações interactivas** | Marcadores `[1][2]` no Tutor + evento SSE `fontes` + painel no frontend |
| **Persistência fontes** | `V27__mensagens_ia_fontes.sql` + `fontes_json` em `mensagens_ia` |
| **SSE resiliência** | Heartbeat 15s, cancelamento ao fechar tab, headers anti-buffer |
| **OCR scans** | Pré-processamento imagem, DPI 250, OCR selectivo páginas, híbridos |
| **Corpo vs pré-texto** | Ignora índice, preâmbulo, exposição de motivos na extracção de artigos |
| **Ordem crescente** | Artigos ordenados por número legal (1, 2, 3…), não por OCR |

## Como aplicar

```bash
# Na raiz do teu projecto (pasta que contém backend/ e frontend/)
unzip -o magistratura-sessao-completa-patches.zip

# Ou manualmente: copiar pastas backend/ e frontend/ por cima das existentes
```

### Atenção a `application.yml`

O ficheiro completo de `backend/src/main/resources/application.yml` está incluído.  
Se tiveres chaves locais (secrets, URLs), **faz merge** em vez de sobrescrever às cegas — pelo menos o bloco `app.pipeline.ocr`.

### Depois de aplicar

1. Reiniciar o backend (Flyway corre **V27**).
2. Frontend: se necessário `npm install` (não há dependências novas).
3. OCR: Tesseract + idioma `por` instalados no host.
4. Reprocessar PDFs importados para beneficiar de OCR + pré-texto + ordem.

### Login de teste (inalterado)

`estudante@magistratura.local` / `123456`

## Ficheiros novos (não existiam no original)

- `backend/.../dto/ia/CitacaoFonteResponse.java`
- `backend/.../db/migration/V27__mensagens_ia_fontes.sql`
- `backend/.../service/pdf/ImageOcrPreprocessor.java`
- `backend/.../service/pdf/PretextoJuridicoDetector.java`

Os restantes são **substituições** de ficheiros já existentes.

## Docs opcionais

- `CITACOES_INTERATIVAS.md`
- `SSE_RESILIENCIA.md`
- `OCR_SCANS_LEGISLATIVOS.md`
- `CORPO_VS_PRETEXTO.md`
