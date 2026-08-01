# Knowledge Layer — Arquitetura

## Visão

```
Tutor IA | Flashcards* | Questões* | Simulados* | Pesquisa | Resumos
                         │
                         ▼
                  KnowledgeService   ← única API pública de conhecimento
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
     RetrievalEngine  IndexingService  findArticle / findByReference
            │                │
   Lexical + Vector     Chunker + EmbeddingProvider
            │                │
            └──────┬─────────┘
                   ▼
              VectorStore
           (NoOp | PgVector)
```

\* Módulos de geração ainda migram gradualmente (ver `KNOWLEDGE_MIGRATION_ROADMAP.md`).

## Fluxo PDF → IA

```
Upload PDF → Validação → Extração → Parser jurídico → Artigos (BD)
    → KnowledgeIndexerStage → KnowledgeLayerIndexer
    → LegalChunker → EmbeddingProvider → VectorStore
    → disponível via KnowledgeService.search / findArticle
    → PromptBuilder (passagens) → ChatProvider
```

## Componentes

| Componente | Pacote | Responsabilidade |
|------------|--------|------------------|
| KnowledgeService | `knowledge.api` | Porta única |
| DefaultKnowledgeService | `knowledge` | Orquestra search/index/cache |
| HybridRetrievalEngine | `knowledge.retrieval` | Lexical + vector |
| RankingService | `knowledge.ranking` | RRF + boosts jurídicos |
| LegalChunker | `knowledge.chunk` | Chunks respeitando estrutura |
| EmbeddingProvider | `knowledge.embedding` | Ollama (extensível) |
| VectorStore | `knowledge.vector` | NoOp / PgVector |
| KnowledgeLayerIndexer | `knowledge.index` | Liga pipeline ao index |

## Regras de dependência

**Consumidores de IA NÃO podem importar:**

- `ArtigoRepository`, `DocumentoRepository`, `DocumentoEmbeddingRepository`
- SQL / pgvector / `VectorStore` concreto

**Podem:**

- `KnowledgeService` e DTOs em `knowledge.api`

**Dentro de `knowledge.*` e `pipeline.*` (documental):** repositories permitidos.

## Decisões

1. Tabela `knowledge_vectors` separada de `documento_embeddings` legado.
2. Default `vector-store=noop` para arranque sem pgvector.
3. Embeddings fail-soft para não bloquear pipeline se Ollama estiver down.
4. `KnowledgeContentKind` distinto de `knowledge.model.KnowledgeKind` (artefactos gerados).
5. Tutor usa `EntityManager.getReference` só para FK ao *persistir* questões/flashcards gerados.

## Configuração

```yaml
app.knowledge.vector-store: noop | pgvector
app.knowledge.embedding-provider: ollama
app.knowledge.embedding-model: nomic-embed-text
app.knowledge.embedding-dimensions: 768
app.knowledge.embedding-fail-soft: true
app.knowledge.indexer: knowledge | noop
```
