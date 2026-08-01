# Magistratura — BD limpa alinhada ao backend

Migrations Flyway compatíveis com as entidades JPA do `magistratura-backend-final`.

## Como aplicar (desenvolvimento)

```bash
# 1. Reset total
psql -U postgres -d magistratura -f RESET_BD.sql

# 2. Copiar migrations para o backend
cp db/migration/*.sql  ../magistratura-backend/src/main/resources/db/migration/

# 3. Arrancar o backend (Flyway aplica tudo)
cd ../magistratura-backend
mvn spring-boot:run
```

## Migrations

| Ficheiro | Conteúdo |
|----------|----------|
| V1__schema.sql | Todas as tabelas alinhadas às Entities |
| V2__constraints.sql | UNIQUE, CHECK |
| V3__indexes.sql | Índices de desempenho |
| V4__seeds.sql | Categorias + utilizador de teste |

## Login de teste

```
email: estudante@magistratura.local
password: 123456
```

**Nota:** o hash BCrypt em V4 deve ser regenerado com o `BCryptPasswordEncoder` do Spring Security antes de usar em produção.
