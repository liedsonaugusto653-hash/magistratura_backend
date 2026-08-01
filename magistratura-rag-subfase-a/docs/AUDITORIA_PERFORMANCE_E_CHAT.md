# Auditoria — Performance pipeline, latência IA, resposta duplicada

## 1) Processamento de documentos lento

### Causa raiz
- `POST /api/documentos/{id}/processar` era **síncrono**: a thread HTTP esperava OCR de todas as páginas.
- `OcrExtractorService` processava páginas **em série** (render + Tesseract).
- OCR a 200 DPI em 103 páginas é inerentemente pesado; o problema de UX era o bloqueio do pedido HTTP.

### Ficheiros
- `DocumentoService.processar` / `reprocessar`
- `DocumentoController`
- `OcrExtractorService`
- `DocumentosView.vue`

### Correção aplicada
1. **Async**: `DocumentoProcessamentoJob` (`@Async("pipelineExecutor")`) — API marca `PROCESSANDO` e devolve de imediato.
2. **OCR paralelo**: pool de workers (default 4) com a mesma DPI/idioma; só a renderização PDFBox é serial (thread-safety).
3. **Frontend**: polling do estado até `PROCESSADO` / `PROCESSADO_COM_AVISOS` / `ERRO` / `FALHA_EXTRACAO`.

### Impacto / risco
- Tempo de parede OCR reduz ~3–4× em CPU multi-core (qualidade igual).
- Pedido HTTP deixa de timeout.
- Risco baixo: jobs concorrentes limitados (pool 2–4); estado PROCESSANDO impede reentrada.

---

## 2) IA mais lenta na app do que no Ollama directo

### Causa raiz (latência *antes* do primeiro token)
Fluxo app: Frontend → JWT → TutorService → **KnowledgeService.search (RAG)** → diplomaRepository → ConversationMemory (histórico até 20 msgs) → PromptBuilder (prompt sistema + passagens) → Ollama.

Terminal Ollama: só o modelo, sem RAG nem DB.

Componentes que adicionam tempo:
| Passo | Ordem de magnitude |
|-------|--------------------|
| Knowledge search / lexical | dezenas–centenas ms |
| Histórico 20 mensagens + system prompt longo | tokens a mais → geração mais lenta |
| Persistência mensagem utilizador | ms |
| SSE encoding JSON por token | desprezável |

Não é “bug”; é custo do RAG + contexto jurídico. Ollama em si não foi alterado.

### Correção aplicada
- Removida consulta `findArticle` redundante quando o `search` já devolveu passagens.

### Não feito (de propósito)
- Não remover RAG.
- Não reduzir qualidade do prompt jurídico.

### Recomendação futura (opcional)
- Cache de search por (mensagem hash + diplomaId) TTL curto.
- Métricas `time to first token` no backend.

---

## 3) IA responde duas vezes

### Causa raiz
`frontend/src/stores/tutor.js` `enviarMensagem`:
1. Streaming conclui e mostra a resposta.
2. Em conversa nova, `selecionarConversa()` recarregava mensagens; se isso (ou outro passo) lançasse, o `catch` caía no **fallback síncrono** `POST /ia/chat`.
3. Fallback só filtrava mensagens com `aEscrever === true`. Após sucesso do stream, `aEscrever` já era `false` → a 1ª resposta **ficava** e a 2ª era **acrescentada**.
4. Resultado: duas bolhas iguais (ou sumiço/reaparecimento ao recarregar).

### Correção aplicada
- Se o stream já entregou tokens → **nunca** chamar fallback síncrono.
- Após stream em conversa nova: actualizar `conversaAtual.id` sem `selecionarConversa` (evita race e 2ª chamada Ollama).
- Guarda `if (this.aEnviar) return` no início.

### Impacto / risco
- Baixo; preserva fallback só quando o stream falha a zero tokens.

---

## Configuração nova

```yaml
app.pipeline.ocr.threads: 4   # PIPELINE_OCR_THREADS
```

## Plano de verificação
1. Processar PDF longo → resposta HTTP imediata + estado PROCESSANDO + logs OCR progresso paralelo.
2. Chat: uma pergunta → uma bolha IA.
3. Comparar first-token app vs CLI (app terá overhead RAG residual esperado).
