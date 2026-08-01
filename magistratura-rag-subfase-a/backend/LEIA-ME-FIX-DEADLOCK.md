# Fix: permission denied to set parameter "deadlock_timeout"

## Causa
O HikariCP corria no arranque:

```sql
SELECT set_config('lock_timeout', '8s', false),
       set_config('deadlock_timeout', '1s', false)
```

O role `magistratura` (e muitos roles de aplicação) **não pode** alterar `deadlock_timeout`.
A ligação falha → Flyway não corre → `entityManagerFactory` não sobe → JWT/Tomcat caem.

## Correcção
`connection-init-sql` passa a definir **só** `lock_timeout` (suficiente para não ficar preso em locks).

## Aplicar
Copia `src/main/resources/application.yml` para o teu `backend/`.

Ou edita manualmente o bloco `spring.datasource.hikari`:

```yaml
hikari:
  connection-init-sql: ${DB_CONNECTION_INIT_SQL:SELECT set_config('lock_timeout', '${DB_LOCK_TIMEOUT:8s}', false)}
  maximum-pool-size: ${DB_POOL_SIZE:10}
  connection-timeout: ${DB_CONNECTION_TIMEOUT_MS:30000}
```

## Se ainda falhar (lock_timeout também negado)
No `.env` ou ambiente:

```bat
set DB_CONNECTION_INIT_SQL=
```

(ou remove a linha `connection-init-sql` do yml)

## Reiniciar
```bat
cd backend
mvn spring-boot:run
```

Confirma que o PostgreSQL está a correr (`docker compose up -d` se usares o compose do projecto).
