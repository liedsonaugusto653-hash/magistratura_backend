# Estratégia de IA — Magistratura

**Estado:** documentação apenas (não implementado nesta entrega).

## Situação atual

- O backend já tem abstração de providers (padrão similar a `AIProvider`).
- Em produção local: **Ollama** com modelo `llama3.2:3b` (configurável via `OLLAMA_MODEL`).
- Fallbacks heurísticos existem nos fluxos de geração quando a IA está offline.

## Adicionar um segundo provider (esforço estimado)

| Passo | Trabalho | Esforço |
|-------|----------|---------|
| 1 | Novo componente que implementa a mesma interface do provider atual (ex.: `OpenAiProvider` ou `GeminiProvider`) | 2–4 h |
| 2 | Configuração em `application.yml` (`app.ai.default-provider`, API keys via env) | 30 min |
| 3 | Factory/seleção do provider ativo (já parcialmente preparada se existir `AIProviderFactory`) | 1–2 h |
| 4 | Timeouts + circuit breaker (padrão Resilience4j, se já no classpath) | 1–2 h |
| 5 | Testes unitários com mock do HTTP client | 1–2 h |
| **Total** | Sem reescrever `TutorService` | **½–1 dia** |

**Recomendação:** manter Ollama como default local; adicionar OpenAI/Gemini só quando houver chave e orçamento. O `TutorService` continua a depender só da interface.

## Requisitos mínimos de servidor — llama3.2:3b (Ollama)

| Cenário | RAM | CPU | Disco | Notas |
|---------|-----|-----|-------|--------|
| Demo / 1 utilizador | 8 GB | 4 vCPU | 20 GB | Latência aceitável em CPU |
| Pequeno grupo (3–5) | 16 GB | 8 vCPU | 40 GB | Preferível quantização Q4/Q5 |
| Com GPU | 8 GB + GPU 6 GB+ VRAM | 4 vCPU | 20 GB | Muito melhor latência |

- Modelo `llama3.2:3b` em quantização típica: ~2–3 GB em disco.
- Timeout configurado: `OLLAMA_TIMEOUT` (default 120 s) — adequado a CPU lenta.
- Para apresentações: pré-carregar o modelo (`ollama pull llama3.2:3b`) e manter o processo Ollama a correr.

## O que NÃO fazer agora

- Não reescrever `TutorService`.
- Não acoplar controllers a um fornecedor concreto.
- Não tornar a IA obrigatória para login, biblioteca ou progresso (já degradam com heurísticas).
