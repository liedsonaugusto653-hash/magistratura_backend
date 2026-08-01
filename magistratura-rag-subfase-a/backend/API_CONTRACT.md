# API Contract — Magistratura Backend

Base URL: `http://localhost:8080/api`  
Autenticação: `Authorization: Bearer {token}` (exceto login)

---

## Autenticação

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| POST | `/auth/login` | Não | Login → JWT |
| POST | `/auth/logout` | Sim | Logout (stateless) |
| GET | `/auth/me` | Sim | Dados do utilizador |

---

## Dashboard

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/dashboard` | Sim | Resumo de progresso |

---

## Biblioteca Jurídica

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/diplomas` | Sim | Lista (filtros: termo, categoriaId) |
| GET | `/diplomas/{id}` | Sim | Detalhe + artigos resumidos |
| GET | `/artigos` | Sim | Lista (filtros: termo, diplomaId) |
| GET | `/artigos/{id}` | Sim | Detalhe completo |
| GET | `/categorias` | Sim | Lista |
| GET | `/categorias/{id}` | Sim | Detalhe |
| GET | `/temas` | Sim | Lista |
| GET | `/temas/{id}` | Sim | Detalhe |

---

## Flashcards

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/flashcards` | Sim | Lista + progresso individual |
| GET | `/flashcards/{id}` | Sim | Detalhe + progresso |
| POST | `/flashcards/{id}/revisar` | Sim | Body: `{ "acertou": true }` |

---

## Questões

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/questoes` | Sim | Lista (sem gabarito) |
| GET | `/questoes/{id}` | Sim | Detalhe (sem gabarito) |
| POST | `/questoes/{id}/responder` | Sim | Body: `{ "resposta": "A" }` |

---

## Simulados

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/simulados` | Sim | Lista disponíveis |
| GET | `/simulados/{id}` | Sim | Detalhe + questões |
| POST | `/simulados/{id}/iniciar` | Sim | Cria tentativa |
| POST | `/simulados/tentativas/{id}/responder` | Sim | Body: `{ "questaoId": "...", "resposta": "B" }` |
| POST | `/simulados/tentativas/{id}/finalizar` | Sim | Calcula pontuação |
| GET | `/simulados/historico` | Sim | Tentativas concluídas |

---

## Estatísticas

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/estatisticas` | Sim | Progresso consolidado (dados reais) |

---

## Tutor IA

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| GET | `/ia/status` | Sim | Disponibilidade do provider |
| GET | `/ia/conversas` | Sim | Lista conversas |
| POST | `/ia/conversas` | Sim | Cria conversa |
| GET | `/ia/conversas/{id}` | Sim | Detalhe + mensagens |
| DELETE | `/ia/conversas/{id}` | Sim | Elimina |
| POST | `/ia/chat` | Sim | Chat síncrono |
| POST | `/ia/chat/stream` | Sim | Chat com SSE |
| POST | `/ia/resumo` | Sim | Resumo de diploma/artigo |
| POST | `/ia/explicar` | Sim | Explicar artigo |
| POST | `/ia/flashcards` | Sim | Gerar flashcards via IA |
| POST | `/ia/questoes` | Sim | Gerar questões via IA |

---

## Login de teste

```
email: estudante@magistratura.local
password: 123456
```
