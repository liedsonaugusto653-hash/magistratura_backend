# Patch backend — questões e simulados (503)

## Problema
- `POST /api/ia/flashcards` → **200** (OK)
- `POST /api/ia/questoes` → **503**
- Simulados falham pelo mesmo parser de questões

Com modelo `llama3.2:3b`, o JSON de questões (mais complexo) costuma vir com markdown, chaves em inglês ou truncado. O parser antigo exigia JSON perfeito em `{"questoes":[...]}` e convertia qualquer falha em `IAIndisponivelException` (503).

## Ficheiros a copiar para o backend

```
src/main/java/ao/magistratura/ia/IaJsonExtractor.java          (NOVO)
src/main/java/ao/magistratura/service/QuestaoService.java      (substituir)
src/main/java/ao/magistratura/service/SimuladoService.java     (substituir)
src/main/resources/prompts/questao.txt                         (substituir)
```

## Depois

```bash
cd backend
mvn -q compile spring-boot:run
# ou o teu fluxo habitual de restart
```

## Frontend (opcional, já no ZIP frontend)

ArtigoView gera **3** questões por defeito (em vez de 5) — mais fiável com modelos 3B.

## Verificação

1. Artigo → Gerar questões → Network `POST /api/ia/questoes` → **200**
2. Simulados → Gerar → `POST /api/simulados/gerar` → **200**

Se ainda 503, vê o log do backend (`Falha ao interpretar questões`) — o trecho da resposta Ollama ajuda a afinar.

## Actualização — GlobalExceptionHandler

O handler antigo **substituía** todas as mensagens de `IAIndisponivelException` por:

> "O Tutor IA está indisponível de momento..."

Isso escondia a causa real (ex.: JSON inválido). O ficheiro actualizado devolve `ex.getMessage()`.

Copiar também:
```
src/main/java/ao/magistratura/exception/GlobalExceptionHandler.java
```
