# Correção — PDF protegido + UX de falha de extracção

**Data:** 2026-07-28  
**Princípio:** não reescrever o pipeline; corrigir a persistência da falha e a orientação ao utilizador.

## Causa raiz (além do PDF em si)

1. O PDF oficial da Assembleia pode vir com restrições (owner password) → classificado `PROTECTED` (já estava correcto).
2. **Bug:** o job, no `catch`, gravava sempre a mensagem genérica *"Não foi possível concluir o processamento."* e estado `ERRO`. A mensagem útil só ia no SSE e **desaparecia após refresh**.
3. `FALHA_EXTRACAO` em memória na stage não era persistida de forma fiável → `marcarErro` sobrescrevia para `ERRO`.

## O que mudou (ficheiros)

### Backend
| Ficheiro | Alteração |
|----------|-----------|
| `PdfLoadHelper.java` | `codigoErro()` estável + mensagens alinhadas com a UI |
| `PdfAnalysisResult.java` | campo `codigoErro` |
| `PdfAnalysisService.java` | preenche `codigoErro` em PDFs protegidos |
| `PdfTextExtractorStage.java` | `marcarFalha` grava `mensagemProgresso` + `tipoPdf` |
| `DocumentoEstadoService.java` | `marcarFalhaExtracao` + `isFalhaExtracao` + mensagem real em `marcarErro` |
| `DocumentoProcessamentoJob.java` | persiste mensagem real; distingue FALHA_EXTRACAO vs ERRO |
| `DocumentoProgressHub.java` | `publishError(..., estado)`; progresso **sem jargão** de etapas; onFalhou com código certo |
| `DocumentoService.java` | pré-inspecção no **import** (não bloqueia; avisa se PROTECTED) |
| `DocumentoResponse.java` | `tipoPdf`, `codigoErro`, `observacoesProcessamento`, `acoesSugeridas` |
| `DocumentoController.java` | mapeia campos novos + passos sugeridos |

### Frontend
| Ficheiro | Alteração |
|----------|-----------|
| `DocumentosView.vue` | badges legíveis; alerta no import se protegido; card de recuperação com passos; progress em estados intermédios |

## Como aplicar

Sobrepor estes ficheiros na árvore do projecto e reiniciar o backend (+ refresh do frontend).

```bash
# Exemplo: a partir da raiz do teu repo
cp -R patch/.../backend/src/main/java/ao/magistratura/... backend/src/main/java/ao/magistratura/
cp patch/.../frontend/src/views/DocumentosView.vue frontend/src/views/
```

## Validação

1. Importar um PDF **protegido** → aparece aviso no sucesso e no cartão (`tipoPdf=PROTECTED`), ainda `IMPORTADO`.
2. Processar → SSE com mensagens legíveis → estado final **`FALHA_EXTRACAO`** (não só ERRO genérico).
3. Refresh da página → a mensagem e os passos (**Imprimir para PDF**) **permanecem**.
4. Exportar sem protecção → importar de novo → processar → `PROCESSADO`.

## O que NÃO se fez (de propósito)

- Não se removeu protecção de PDFs no servidor (decisão legal/produto).
- Não se reescreveu o orchestrator nem o modelo de stages.
- Não se alterou o contrato JWT / autenticação / RAG.

## Actualização — PDFs do Diário da República (só imagem + copy:no, print:yes)

O ficheiro `Lei-Geral-do-Trabalho-2023.pdf` (Jurisnet / Ghostscript) é:

- **103 páginas = 103 imagens** a página inteira (não há camada de texto)
- Encriptado: `copy:no`, **`print:yes`**

### Bug adicional corrigido
A análise marcava `PROTECTED` + `ocrNecessario=false` sempre que `encriptado && !extract`, **mesmo com print permitido**. O OCR era bloqueado à partida.

### Correcção
| Situação | Antes | Agora |
|----------|-------|-------|
| copy:no, print:yes, páginas-imagem | PROTECTED, sem OCR | **IMAGE**, OCR activo |
| sem print e sem extract | PROTECTED | PROTECTED (inalterado) |

Ficheiros extra nesta ronda:
- `OcrExtractorService.java` — deixa de recusar OCR só por `!extract` se `print` for permitido
- `PdfAnalysisService.java` — classifica o caso DR/Jurisnet como IMAGE
- `PdfTextExtractorStage.java` — se a extracção nativa falhar mas OCR estiver planeado, continua

### Requisitos no servidor
```bash
# Debian/Ubuntu
sudo apt install tesseract-ocr tesseract-ocr-por
# Verificar
tesseract --list-langs   # deve listar "por"
```

`app.pipeline.ocr.enabled=true` e `app.pipeline.ocr.language=por` (já é o default no código).

### Expectativa de desempenho
OCR a 200 DPI em 103 páginas pode demorar vários minutos. O progresso SSE («A ler página X de 103») deve actualizar. DPI configurável: `app.pipeline.ocr.dpi`.
