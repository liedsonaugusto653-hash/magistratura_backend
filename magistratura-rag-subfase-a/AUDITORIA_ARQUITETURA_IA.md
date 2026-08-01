# Auditoria e Refatoração Arquitetural — IA / RAG

**Data:** 2026-08-01  
**Âmbito:** desacoplamento do fornecedor de IA, separação pesquisa→contexto→geração, redução de tokens.

## Nota geral

A base já estava forte (Java 21, Flyway `validate`, `AIProvider`, Knowledge Layer com retrieval/ranking/vector).  
Esta entrega **fecha o acoplamento residual ao Ollama** e formaliza a montagem de contexto.

## Problemas encontrados

| Problema | Impacto |
|----------|---------|
| Único bean `OllamaProvider` sempre activo | Impossível trocar Gemini/OpenAI sem alterar código |
| Resolução de contexto embutida em `TutorService` | SRP; dificulta reutilizar política de tokens |
| `MAX_CHARS_POR_PASSAGEM = 3500` × 5 passagens | Risco de prompts grandes e custo elevado |
| Embeddings e chat partilham implicitamente “Ollama” | Confusão operacional (já havia `embedding-provider`, faltava `ia.provider`) |

## Solução implementada

### 1. Multi-provider (`app.ia.provider`)

```
AIProvider (interface — inalterada no contrato)
├── OllamaProvider      @ConditionalOnProperty = ollama (default)
├── GeminiProvider      = gemini
├── OpenAiCompatibleProvider = openai
└── OpenRouterProvider  = openrouter  (API OpenAI-compatible)
```

**Troca sem recompilar serviços:**

```bash
# .env / ambiente
IA_PROVIDER=gemini
GEMINI_API_KEY=...

# ou
IA_PROVIDER=openai
OPENAI_API_KEY=...
```

`TutorService`, `FlashcardService`, `QuestaoService`, etc. **continuam a injectar só `AIProvider`**.

### 2. `ContextAssemblyService`

Fluxo canónico:

```
ChatRequest
  → KnowledgeService.search (retrieval + ranking)
  → StudyContextPolicy.limitar + truncarTextos
  → ContextoJuridico
  → PromptBuilder / AIProvider
```

`TutorService` delega `resolverContextoComRetrieval` e `mapearFontes`.

### 3. Política de tokens mais agressiva

| Parâmetro | Antes | Depois |
|-----------|-------|--------|
| `CHAT_MAX_PASSAGENS_NO_PROMPT` | 5 | 4 |
| `MAX_CHARS_POR_PASSAGEM` | 3500 | 2200 |

Estimativa: **~50% menos caracteres** no bloco de fontes do prompt de chat, mantendo citações `[n]`.

### 4. `KnowledgePassage` helpers

`titulo()`, `referencia()`, `excerpt()`, `comTextoTruncado()` — apoio a UI e truncagem sem mutação.

## O que NÃO foi alterado (de propósito)

- Endpoints públicos e contratos JSON
- Regras jurídicas / prompts de domínio (apenas consumo de contexto)
- Pipeline PDF / OCR
- Flyway / modelo de dados
- Frontend

## Ficheiros novos

- `ia/gemini/GeminiProvider.java`
- `ia/openai/OpenAiCompatibleProvider.java`
- `ia/openai/OpenRouterProvider.java`
- `ia/ContextAssemblyService.java`

## Ficheiros alterados

- `ia/ollama/OllamaProvider.java` — `@ConditionalOnProperty`
- `ia/ollama/OllamaConcurrencyLimiter.java` — idem
- `service/TutorService.java` — delegação ao `ContextAssemblyService`
- `knowledge/api/StudyContextPolicy.java` — limites + `truncarTextos`
- `knowledge/api/KnowledgePassage.java` — helpers
- `resources/application.yml` — `app.ia`, `app.gemini`, `app.openai`, `app.openrouter`

## Impacto

| Dimensão | Efeito |
|----------|--------|
| Arquitectura | Independência do fornecedor de IA |
| Tokens | Menos passagens e truncagem mais curta |
| Escalabilidade | Mesmo código de negócio para qualquer provider |
| Manutenção | Contexto RAG num único serviço |
| Comportamento funcional | Preservado (mesmos endpoints e fluxos) |

## Próximos passos recomendados (não feitos agora)

1. Streaming nativo Gemini (`streamGenerateContent`) em vez de fallback por blocos  
2. Cache de `KnowledgeResult` por hash da query (Caffeine)  
3. Extrair geração de flashcards/questões para um `StudyGenerationService` (OntologiaService ainda é grande)  
4. Índices DB se `EXPLAIN` mostrar sequential scans em artigos/relações  

## Como validar

```bash
# Default Ollama
mvn -pl backend test

# Gemini
IA_PROVIDER=gemini GEMINI_API_KEY=xxx mvn spring-boot:run

# Verificar bean activo
GET /api/ia/status  # se existir — nome do provider
```
