# Roadmap — migração dos módulos para KnowledgeService

## Estado atual (Missão 3.3)

| Módulo | Leitura de conhecimento | Persistência de domínio |
|--------|-------------------------|-------------------------|
| **Tutor chat** | `KnowledgeService.search` | Conversas / mensagens |
| **Explicar artigo** | `KnowledgeService.findArticle` | — |
| **Gerar flashcards (prompt)** | `KnowledgeService` (passagens) | `FlashcardRepository` + diploma FK |
| **Gerar questões (prompt)** | `KnowledgeService` (passagens) | `QuestaoRepository` + artigo via `EntityManager.getReference` |
| **Simulados** | Ainda não via KS | Serviços próprios |
| **Biblioteca CRUD** | Repositories (correcto — domínio documental) | Repositories |

## Estado futuro desejado

```
Flashcards / Questões / Simulados / Pesquisa / Resumos
        ↓
  KnowledgeService.search / findArticle / findByReference
        ↓
  KnowledgeResult (sem entidades JPA no prompt)
```

## Estratégia de migração

### Fase A (feita)
- Chat e explicar artigo 100% KnowledgeService para leitura.
- Remover `ArtigoRepository` do `TutorService`.

### Fase B
- Endpoints de *pesquisa* de flashcards/questões por tema jurídico → `KnowledgeService.search`.
- Não alterar SRS/UI de criação manual.

### Fase C
- Geradores automáticos do pipeline (`FlashcardGenerator`, `QuestionGenerator`) a usar passagens indexadas em vez de `Artigo` completo quando possível.

### Fase D
- Simulados: montagem de provas a partir de chunks rankeados (`KnowledgeQuery` com filtros de diploma/tema).

### Regras
- Nunca expor `VectorStore` / SQL fora de `knowledge.*`.
- Persistência de entidades de estudo (Flashcard, Questao) continua nos seus services/repos — só a **fonte de texto jurídico** vem da Knowledge Layer.
