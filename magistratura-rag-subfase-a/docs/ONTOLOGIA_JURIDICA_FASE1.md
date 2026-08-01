# Ontologia Jurídica — Fase 1 (incremental)

## Objectivo

Introduzir a **camada conceptual** sem reescrever diplomas, RAG, pipeline ou frontend existente.

```
Camada documental (já existe)     Camada conceptual (nova)
diplomas / artigos / PDFs    ←→   entidades → tópicos → relações
         │                                    │
         └──────── topico_artigo ─────────────┘
```

O estudante passa a poder estudar por **conceito** (ex.: Empregador, Responsabilidade do Estado), e o sistema agrega artigos de vários diplomas nessa página.

## O que esta entrega inclui

| Peça | Ficheiros |
|------|-----------|
| Migration Flyway **V28** | `entidades_juridicas`, `topicos_juridicos`, `relacoes_juridicas`, `topico_artigo` + seed |
| Entidades JPA | `EntidadeJuridica`, `TopicoJuridico`, `RelacaoJuridica`, `TopicoArtigo` |
| Repositórios | 4 interfaces Spring Data |
| DTOs | `dto/ontologia/*` |
| Serviço + API | `OntologiaService`, `OntologiaController` (`/api/ontologia/**`) |

**Não altera:** Tutor, KnowledgeService, pipeline PDF, categorias/temas legados, frontend.

## Seed conceptual (amostra)

**Entidades:** ESTADO, PESSOA, FAMILIA, PROPRIEDADE, CONTRATO, RESPONSABILIDADE, TRABALHO, CRIME, PROCESSO_CIVIL, PROCESSO_PENAL, ADMINISTRACAO, TRIBUNAL.

**Tópicos exemplo:** `ESTADO.PODER_JUDICIAL`, `PESSOA.CAPACIDADE`, `TRABALHO.EMPREGADOR`, `PROCESSO_PENAL.PRISAO_PREVENTIVA`, …

**Relações exemplo:** PRESSUPOE, CONEXO, ESPECIALIZA.

## API (JWT obrigatório)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/ontologia/entidades` | Lista entidades |
| GET | `/api/ontologia/entidades/{id}` | Detalhe |
| GET | `/api/ontologia/entidades/codigo/{codigo}` | Por código (`ESTADO`) |
| GET | `/api/ontologia/entidades/{id}/mapa` | Mapa: entidade + tópicos + artigos |
| GET | `/api/ontologia/entidades/{id}/topicos` | Tópicos da entidade |
| GET | `/api/ontologia/topicos?termo=` | Pesquisa textual |
| GET | `/api/ontologia/topicos/{id}` | Detalhe + relações |
| GET | `/api/ontologia/topicos/{id}/artigos` | Artigos ligados |
| POST | `/api/ontologia/topicos/{id}/artigos` | Ligar artigo `{ "artigoId": "…" }` |
| DELETE | `/api/ontologia/topicos/{topicoId}/artigos/{artigoId}` | Desligar |

### Exemplo — ligar artigo a tópico

```http
POST /api/ontologia/topicos/{topicoId}/artigos
Authorization: Bearer …
Content-Type: application/json

{
  "artigoId": "uuid-do-artigo",
  "relevancia": 1.0,
  "origemLigacao": "MANUAL"
}
```

## Como aplicar

1. Copiar o conteúdo de `backend/src/main/` por cima do teu `backend/src/main/`.
2. Reiniciar o backend (Flyway corre **V28**).
3. Confirmar no Swagger o tag **Ontologia Jurídica**.
4. Ligar manualmente artigos já processados aos tópicos (POST acima).

Se a base local já tiver migrations > 28, renomeia o ficheiro para o próximo número livre (ex. `V29__…`).

## Fases seguintes (não nesta entrega)

| Fase | Conteúdo |
|------|----------|
| **2** | UI «Mapa Jurídico» no frontend (lista entidades → mapa) |
| **3** | `KnowledgeQuery` com `topicoId` / expansão por relações no RAG |
| **4** | Sugestão automática de ligações (IA ou keywords) após processar PDF |
| **5** | Percursos de estudo (`PRESSUPOE`) e mapas mentais |

## Princípios

1. **Diplomas continuam a ser a fonte normativa** — a ontologia não inventa texto legal.
2. **Códigos estáveis** (`ESTADO`, `TRABALHO.EMPREGADOR`) para integração e import/export.
3. **Grafo leve** — relações tipadas, sem OWL/RDF na Fase 1.
4. **Legado intacto** — `categorias` / `temas` mantêm-se; ponte opcional via `topicos_juridicos.categoria_id`.
