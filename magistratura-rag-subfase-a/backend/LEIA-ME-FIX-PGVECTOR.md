# Fix: extension "vector" is not available (Flyway V21)

## Causa
`V21__knowledge_layer_vectors.sql` faz `CREATE EXTENSION vector`, mas o teu PostgreSQL **não tem pgvector** instalado (Postgres stock / imagem sem a extensão).

## Solução A — migration tolerante (incluída neste ZIP)
Substitui:
- `backend/src/main/resources/db/migration/V21__knowledge_layer_vectors.sql`
- `backend/src/main/resources/db/migration/V25__knowledge_layer_vectors.sql`

Sem pgvector cria tabelas com `REAL[]`. Com `app.knowledge.vector-store=noop` (default) a app sobe normalmente.

### Reparar Flyway (obrigatório após a falha)
A V21 falhou a meio — limpa o registo falhado **antes** de voltar a correr:

```sql
-- psql -U magistratura -d magistratura
DELETE FROM flyway_schema_history WHERE version = '21' AND success = false;
-- se existir versão 21 marcada mas tabelas incompletas:
-- DELETE FROM flyway_schema_history WHERE version = '21';
```

No PowerShell (Docker):

```powershell
docker exec -it magistratura-postgres psql -U magistratura -d magistratura -c "DELETE FROM flyway_schema_history WHERE version = '21' AND success = false;"
```

Depois:

```powershell
cd backend
mvn spring-boot:run
```

## Solução B — usar imagem com pgvector (recomendado a médio prazo)
No `backend/docker-compose.yml` a imagem deve ser:

```yaml
image: pgvector/pgvector:pg16
```

**Atenção:** mudar a imagem num volume antigo não instala a extensão magicamente. Ou:

```powershell
docker compose down
# só se puderes apagar dados de dev:
docker volume rm magistratura-rag-subfase-a_postgres-data
# ou o nome do volume que o compose criou
docker compose up -d
```

Depois `CREATE EXTENSION vector` funciona e podes usar `KNOWLEDGE_VECTOR_STORE=pgvector` mais tarde.

## Confirmar noop
No `application.yml` / env:

```yaml
app.knowledge.vector-store: noop
```

ou

```powershell
$env:KNOWLEDGE_VECTOR_STORE="noop"
```
