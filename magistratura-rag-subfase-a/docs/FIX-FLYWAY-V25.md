# Corrigir arranque Flyway (migração 21 / pgvector)

## Causa do erro

```
Detected resolved migration not applied to database: 21.
```

A migração de vectores foi activada como **V21**, mas a base de dados **já tinha** V22, V23, V24 aplicadas.
O Flyway recusa migrar “para trás” no histórico.

## Correção (já no patch)

1. **Remover** `V21__knowledge_layer_vectors.sql` (se existir).
2. **Usar** `V25__knowledge_layer_vectors.sql` (depois de V24).
3. `spring.flyway.out-of-order: true` no `application.yml`.

## No teu PC (Windows)

Na pasta `backend/src/main/resources/db/migration/`:

```text
# APAGA se existir:
V21__knowledge_layer_vectors.sql
V21__knowledge_layer_vectors.sql.disabled

# DEVE existir:
V25__knowledge_layer_vectors.sql
```

Copia o ficheiro do ZIP `magistratura-flyway-v25-fix.zip`.

## PostgreSQL com pgvector

A V25 executa `CREATE EXTENSION vector`.

### Opção A — Docker (recomendado)

```bash
# docker-compose com imagem:
# image: pgvector/pgvector:pg16
```

### Opção B — Postgres local sem pgvector

Temporariamente:

```powershell
$env:KNOWLEDGE_VECTOR_STORE="noop"
# e renomeia V25 para não correr:
# V25__knowledge_layer_vectors.sql → V25__knowledge_layer_vectors.sql.disabled
```

Depois instala pgvector ou muda para a imagem Docker e volta a activar V25.

## Arranque

```powershell
cd backend
mvn spring-boot:run
```

Deves ver Flyway a aplicar **V25** com sucesso.
