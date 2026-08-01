# Expansão PRESSUPOE no RAG

## Ideia

Quando a consulta ao Knowledge Layer traz `topicoId` (estudo por conceito),

```
RESPONSABILIDADE.INDEMNIZACAO  ──PRESSUPOE──►  RESPONSABILIDADE.PRESSUPOSTOS
```

o sistema inclui **também** artigos ligados ao tópico pressuposto, com score reduzido.

Assim o Tutor / flashcards / questões recebem contexto jurídico pré-requisito sem o estudante o pedir explicitamente.

## Fluxo

```
KnowledgeQuery(topicoId=A, texto=…)
        │
        ├─ artigos ligados a A          (metodo=ONTOLOGIA, factor=1.0)
        │
        └─ BFS PRESSUPOE (origem→destino)
              artigos de B, C…          (metodo=ONTOLOGIA_PRESSUPOE, factor=0.65^nível)
        │
        ▼
   fundir com retrieval lexical/vector (se houver texto)
        │
        ▼
   KnowledgeResult (estrategia …+ONTOLOGIA+PRESSUPOE)
```

## Configuração

```yaml
app:
  ontologia:
    rag-expandir-pressupoe: true
    rag-expansao-profundidade: 1
    rag-expansao-factor-score: 0.65
    rag-expansao-max-topicos: 8
```

Desligar: `ONTOLOGIA_RAG_PRESSUPOE=false`.

## Ficheiro

`DefaultKnowledgeService.java` — métodos `passagesOntologiaExpandida` e `expandirPressupoe`.

## Nota

Só expandem relações com `tipo_relacao = PRESSUPOE` (case-insensitive).  
`CONEXO` / `ESPECIALIZA` não entram nesta versão (fácil de alargar depois).
