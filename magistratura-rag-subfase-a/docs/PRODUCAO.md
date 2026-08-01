# Magistratura — checklist de publicação

## Alterações desta versão

1. **pgvector activo** — migração `V21__knowledge_layer_vectors.sql` e default `app.knowledge.vector-store=pgvector`
2. **Postgres com pgvector** — imagem `pgvector/pgvector:pg16` no Docker Compose
3. **Rate limit de IA** — máx. 20 POST/min por utilizador (JWT) ou IP em `/api/ia/**` e `/api/simulados/**`
4. **Concorrência Ollama** — máx. 2 pedidos simultâneos (chat + embeddings), configurável

## Pré-requisitos

```bash
# Ollama com modelo de chat e de embeddings
ollama pull llama3.2:3b
ollama pull nomic-embed-text
```

PostgreSQL **com extensão vector** (não uses `postgres:alpine` puro).

## Variáveis de ambiente (produção)

| Variável | Default | Notas |
|----------|---------|--------|
| `KNOWLEDGE_VECTOR_STORE` | `pgvector` | `noop` só para CI sem GPU/pgvector |
| `OLLAMA_MAX_CONCURRENT` | `2` | Aumentar se houver GPU dedicada |
| `IA_RATE_LIMIT_RPM` | `20` | Pedidos IA por minuto |
| `IA_RATE_LIMIT_ENABLED` | `true` | |
| `JWT_SECRET` | — | **Obrigatório** alterar |
| `DB_PASSWORD` | — | **Obrigatório** alterar |

## Arranque

```bash
cd deploy
# garante JWT_SECRET e passwords no .env
docker compose -f docker-compose.prod.yml up -d --build
```

Após o primeiro boot com pgvector, **reprocessa** documentos já indexados (ou reimporta) para preencher `knowledge_vectors`.

## Desenvolvimento local sem pgvector

```bash
export KNOWLEDGE_VECTOR_STORE=noop
```

## Verificação

```bash
# extensão
docker compose exec db psql -U magistratura -c "SELECT extname FROM pg_extension WHERE extname='vector';"

# métricas / health
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/prometheus | head
```

## Limitações honestas

- Ollama num único host continua a ser o gargalo de escala horizontal.
- Rate limit em memória: com várias réplicas do backend, usar Redis/Bucket4j.
- Reindexação em massa de embeddings após activar pgvector é necessária para RAG semântico completo.
