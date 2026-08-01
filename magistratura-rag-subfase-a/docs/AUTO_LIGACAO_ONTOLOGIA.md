# Auto-ligação tópico ↔ artigo (opção 2)

## Objectivo

Depois de extrair artigos de um PDF, o sistema **sugere e grava** ligações à ontologia conceptual (ex.: artigo sobre prisão preventiva → `PROCESSO_PENAL.PRISAO_PREVENTIVA`), sem chamar o LLM.

## Onde corre

```
… → PERSISTINDO_ARTIGOS → INDEXANDO → LIGANDO_ONTOLOGIA → GERANDO_CONHECIMENTO → …
```

Etapa nova: `OntologiaLigacaoStage` (`PipelineEtapa.LIGANDO_ONTOLOGIA`).

- **Fail-soft**: falhas não interrompem o pipeline.
- `origem_ligacao = AUTO` nas linhas criadas.
- Não duplica ligações já existentes.

## Algoritmo (determinístico)

1. Indexa tokens dos tópicos activos (nome, descrição, código).
2. Para cada artigo, pontua sobreposição no título (peso alto), capítulo/secção e texto.
3. Aceita se `score >= app.ontologia.min-score` (default `0.42`).
4. Guarda no máximo `max-ligacoes-por-artigo` (default `3`) por artigo.

Não usa embeddings nem Ollama — funciona offline e é rápido.

## Configuração

```yaml
app:
  ontologia:
    auto-ligar: true          # false desliga a etapa
    min-score: 0.42
    max-ligacoes-por-artigo: 3
```

Variáveis: `ONTOLOGIA_AUTO_LIGAR`, `ONTOLOGIA_MIN_SCORE`, `ONTOLOGIA_MAX_POR_ARTIGO`.

Snippet: `backend/src/main/resources/application-ontologia.snippet.yml` (fazer merge no `application.yml`).

## API manual

```http
POST /api/ontologia/auto-ligar
Authorization: Bearer …
Content-Type: application/json

{
  "documentoId": "<uuid>",   // ou diplomaId
  "dryRun": true             // true = só sugerir; false = gravar
}
```

Resposta: lista de `SugestaoLigacaoResponse` (`estado`: `SUGESTAO` | `CRIADA` | `JA_EXISTIA`).

## Ficheiros

| Ficheiro | Papel |
|----------|--------|
| `OntologiaAutoLigacaoService.java` | Matching + persistência |
| `OntologiaLigacaoStage.java` | Etapa do pipeline |
| `PipelineEtapa.java` | + `LIGANDO_ONTOLOGIA` |
| `DocumentPipelineOrchestrator.java` | Inclui a etapa |
| `OntologiaController.java` | `POST /auto-ligar` |
| DTOs `AutoLigarRequest`, `SugestaoLigacaoResponse` |

## Validar

1. Merge config + código; reiniciar backend.
2. Processar um PDF com artigos cujo texto fale de “empregador”, “prisão preventiva”, etc.
3. Log: `Etapa LIGANDO_ONTOLOGIA: N ligação(ões)`.
4. Mapa Jurídico → tópico → artigos aparecem.
5. Ou `POST /api/ontologia/auto-ligar` com `dryRun: true` para ver scores antes de gravar.

## Limitações (próximas iterações)

- Matching lexical só — pode falhar em sinónimos raros.
- Melhorias futuras: embeddings de tópicos, expansão por relações `PRESSUPOE`, revisão humana no frontend.
