# Auditoria Parte 2 — Cache RAG, índices, SRP, contexto estruturado

**Data:** 2026-08-01

## 1. Cache inteligente do RAG (Caffeine)

**Problema:** `ConcurrentHashMap` sem TTL — entradas eternas até `CACHE_MAX` ou invalidação total.

**Solução:**
- Dependências `spring-boot-starter-cache` + `caffeine`
- `CacheConfig` com caches nomeados (`ragQueries`, `artigoById`)
- `DefaultKnowledgeService`: cache Caffeine **120s TTL**, **512** entradas, stats
- Config: `app.cache.rag-ttl-seconds`, `app.cache.rag-max-size`

Invalidação em `indexArtigo` / `removeByDocumento` mantida (`invalidateAll`).

## 2. Optimização de consultas

| Antes | Depois |
|-------|--------|
| `findByReference` carregava **todos** os artigos do diploma e filtrava em memória | `buscarPorNumeroEDiplomaId` (query + índice) |
| Índices dispersos | **V32__perf_indexes_rag.sql**: `artigos(diploma_id, lower(numero))`, documentos por estado/diploma, mensagens por conversa+ts, tópicos, FTS título |

## 3. SRP — OntologiaService

- Novo **`OntologiaFichaService`**: geração IA da ficha de estudo + parse JSON
- `OntologiaService.gerarFichaEstudo` apenas orquestra e mapeia DTO
- Removidas dependências IA desnecessárias do serviço de leitura/mapa

## 4. Contexto jurídico estruturado

**`StructuredContextComposer`** + orçamento `Budget.chatDefault()`:

| Slot | Quota chat |
|------|------------|
| Definição | 1 |
| Princípio | 1 |
| Artigo | 2 |
| Relação | 1 |
| Jurisprudência | 1 |

Classificação heurística por `kind` + texto (princípio, “entende-se por”, “nos termos do”, etc.).  
Integrado em `ContextAssemblyService` **antes** de limitar/truncar.

## Ficheiros

**Novos:** `CacheConfig`, `StructuredContextComposer`, `OntologiaFichaService`, `V32__perf_indexes_rag.sql`  
**Alterados:** `pom.xml`, `DefaultKnowledgeService`, `ContextAssemblyService`, `OntologiaService`, `application.yml`

## Impacto

| Área | Efeito |
|------|--------|
| Carga RAG | Menos retrieval repetido (TTL 2 min) |
| Tokens | Mix de papéis → respostas mais estáveis com o mesmo orçamento |
| DB | Lookups artigo por número sem full scan do diploma |
| Manutenção | Ficha de estudo isolada da navegação ontológica |

## Próximos passos (Parte 3, opcional)

- Redis quando houver >1 instância
- Streaming nativo Gemini
- Partir `DocumentoService` (import vs consulta vs eliminação)
- Orçamento por tokens reais (tokenizer) em vez de só caracteres
