# Correção aplicada

## Erro reportado
```
DocumentoService.java:[96,32] cannot find symbol: findByHashFicheiro(String)
DocumentoService.java:[212,39] cannot find symbol: pesquisar(String, Pageable)
DocumentoService.java:[215,39] cannot find symbol: findByDiplomaId(UUID, Pageable)
DocumentoService.java:[218,39] cannot find symbol: findByCategoriaId(UUID, Pageable)
```

## Causa
`DocumentoRepository.java` só tinha o método `findAll` sobrescrito. Os
outros 4 métodos que o `DocumentoService` já chamava nunca chegaram a ser
declarados na interface.

## Ficheiro alterado
`src/main/java/ao/magistratura/repository/DocumentoRepository.java`

Foram adicionados, seguindo o mesmo padrão já usado em `ArtigoRepository`
e `DiplomaRepository`:
- `findByHashFicheiro(String)` — deteta PDFs duplicados no import
- `findByDiplomaId(UUID, Pageable)`
- `findByCategoriaId(UUID, Pageable)`
- `pesquisar(String, Pageable)` — query JPQL por título/fonte

Nenhum outro ficheiro foi tocado. Confirmei por grep que todos os outros
métodos de repositório chamados pelos services já existem nas interfaces
correspondentes — este era o único ponto de falha.

## Atualização — 403 ao abrir o Swagger UI

`SecurityConfig.java` libertava `/swagger-ui/**` mas não `/swagger-ui.html`
em si — que é a página de entrada real, antes do redirecionamento interno
para `/swagger-ui/index.html`. Corrigido: adicionada essa rota à lista
`ROTAS_PUBLICAS`.

Foi acrescentado o que faltava para o botão "Processar" (no frontend)
funcionar:

- **`dto/biblioteca/DiplomaRequest.java`** (novo) — DTO de entrada:
  `numero`, `titulo` (obrigatórios), `descricao`, `dataPublicacao`,
  `categoriaId` (opcionais).
- **`service/BibliotecaService.java`** — adicionado o método
  `criarDiploma(DiplomaRequest dto)`, que valida a categoria (se indicada)
  e grava o diploma com estado `VIGENTE` por omissão.
- **`controller/DiplomaController.java`** — adicionado
  `POST /api/diplomas`, que devolve `201 Created` com o diploma criado.

### Como criar um diploma (exemplo via Postman/curl)
```
POST /api/diplomas
Authorization: Bearer <token>
Content-Type: application/json

{
  "numero": "CRA/2010",
  "titulo": "Constituição da República de Angola",
  "descricao": "Lei Suprema e Fundamental da República de Angola",
  "dataPublicacao": "2010-02-05",
  "categoriaId": "<uuid da categoria Constitucional, opcional>"
}
```

Depois de criado, o diploma passa a aparecer no seletor "Escolhe o
diploma…" do ecrã "Importar Documentos" no frontend, e o botão "Processar"
já pode ser usado normalmente.
