# Tutor IA — Arquitetura e Endpoints

Este documento descreve o módulo do Tutor IA implementado em `ao.magistratura.ia`,
`ao.magistratura.service.TutorService` e `ao.magistratura.controller.TutorController`.
Deve ser movido para `docs/ai.md` na raiz do projeto quando os três repositórios
(frontend, backend, database) forem consolidados num único `magistratura/`.

## Objetivo

O Tutor IA nunca responde "às cegas". Toda pergunta, resumo, explicação de artigo,
geração de flashcards ou de questões é ancorada em conteúdo real da biblioteca
jurídica (diploma, artigo, resumo ou um trecho selecionado pelo estudante),
injetado no prompt pelo `PromptBuilder`.

## Arquitetura

```
ao.magistratura.ia/
├── AIProvider              interface — contrato que desacopla o sistema do Ollama
├── ChatMessage / ChatRole  modelo de mensagem agnóstico de provider
├── PromptBuilder           carrega templates de resources/prompts/*.txt e injeta contexto jurídico
├── ConversationMemory      persiste/lê ConversaIa e MensagemIa, gere a janela de contexto
├── StreamingService        liga o streaming do AIProvider a um SseEmitter (Server-Sent Events)
└── ollama/
    └── OllamaProvider      implementação de AIProvider para o Ollama (POST /api/chat)
```

### Por que não depende do Ollama diretamente

Nenhuma classe fora de `ia/ollama` conhece detalhes da API do Ollama.
`TutorService` depende apenas da interface `AIProvider`. Para trocar por
OpenAI ou Gemini no futuro, basta:

1. Criar `OpenAIProvider implements AIProvider` (ou `GeminiProvider`).
2. Anotar apenas essa nova classe com `@Component` e marcar o `OllamaProvider`
   como `@ConditionalOnProperty` (ou remover o `@Component` dele), para que o
   Spring injete o novo bean em `TutorService` sem alterar mais nada.

### Streaming

`OllamaProvider.chatStream(...)` lê a resposta NDJSON do Ollama linha a linha
(usando `java.net.http.HttpClient`, sem dependências reativas adicionais) e
invoca um callback por token. `StreamingService` liga esses callbacks a um
`SseEmitter`, correndo o trabalho de rede numa virtual thread para não
bloquear o pool de threads do Tomcat.

### Prompts

Os templates ficam em `src/main/resources/prompts/`:

- `tutor.txt` — prompt de sistema do chat conversacional
- `resumo.txt` — geração de resumos
- `flashcard.txt` — geração de flashcards (força saída em JSON estrito)
- `questao.txt` — geração de questões de escolha múltipla (força saída em JSON estrito)

`PromptBuilder` faz cache em memória do conteúdo dos templates e substitui os
placeholders `{{contexto_juridico}}` e `{{quantidade}}`.

### Preparação para RAG

A tabela `documento_embeddings` (já criada em `V1__schema.sql`) tem agora a
entidade `DocumentoEmbedding` correspondente. Não há ainda geração de
embeddings nem pesquisa semântica — isso fica para uma fase seguinte, quando
um modelo de embeddings for escolhido (ex.: via Ollama `nomic-embed-text`).

## Endpoints

Todos exigem JWT (`Authorization: Bearer <token>`), como o resto da API.

| Método | Endpoint                  | Descrição                                             |
|--------|----------------------------|--------------------------------------------------------|
| GET    | `/api/ia/conversas`        | Lista as conversas do estudante, mais recentes primeiro |
| POST   | `/api/ia/conversas`        | Cria uma nova conversa                                 |
| GET    | `/api/ia/conversas/{id}`   | Devolve uma conversa com o histórico completo           |
| DELETE | `/api/ia/conversas/{id}`   | Elimina uma conversa                                    |
| POST   | `/api/ia/chat`              | Envia uma mensagem, devolve a resposta completa (sem streaming) |
| POST   | `/api/ia/chat/stream`       | Envia uma mensagem, transmite a resposta via SSE (token a token) |
| POST   | `/api/ia/resumo`            | Gera um resumo de um diploma, artigo ou texto livre     |
| POST   | `/api/ia/explicar`          | Explica um artigo (ou trecho selecionado)               |
| POST   | `/api/ia/flashcards`        | Gera flashcards; `guardar=true` persiste-os              |
| POST   | `/api/ia/questoes`          | Gera questões de escolha múltipla; `guardar=true` persiste-as |
| GET    | `/api/ia/status`            | Diagnóstico: nome do provider ativo e disponibilidade    |

### Exemplo — chat com streaming

```
POST /api/ia/chat/stream
Authorization: Bearer <token>
Content-Type: application/json

{
  "conversaId": null,
  "mensagem": "O que é a prisão preventiva?",
  "diplomaId": "uuid-do-codigo-processo-penal",
  "artigoId": null
}
```

Resposta (Server-Sent Events):

```
event: token
data: A

event: token
data:  prisão

event: token
data:  preventiva

event: concluido
data: true
```

## Configuração

Já presente em `application.yml` (Fase 1), sem alterações necessárias:

```yaml
app:
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:llama3.2}
    timeout-seconds: ${OLLAMA_TIMEOUT:120}
```

## O que falta (fora do âmbito desta fase)

- Ligar o Sistema de PDFs (marcadores, notas, destaques, "explicar trecho selecionado")
  ao `artigoContexto`/`trecho` já suportados pelo Tutor.
- Geração real de embeddings para popular `documento_embeddings` e ativar RAG.
- Frontend: `TutorStore`, `TutorService` (Axios) e a view de chat (`views/tutor`).
