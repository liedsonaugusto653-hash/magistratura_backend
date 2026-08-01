# Entrega final — Frontend Magistratura

## 1. Resumo da entrega

Pacote completo do frontend Vue 3 (Vite + Pinia + Vue Router + Axios), com os módulos 1–6 integrados:

- geração e persistência de **flashcards** e **questões** (`guardar: true` por omissão);
- **simulados** com `POST /api/simulados/gerar` e UI de criação;
- **biblioteca** com pesquisa e PDF preview melhorado;
- **Design System** em `src/components/ui/`;
- migração residual de views principais.

**Backend não está incluído** — continua a correr no teu ambiente (Spring + PostgreSQL + Ollama).

## 2. Módulos implementados

| Módulo | Conteúdo |
|--------|----------|
| 1 Flashcards | API + feedback + empty state + labels |
| 2 Questões | Idem + banco de questões |
| 3 Simulados | `simuladoService.gerar`, store `aGerar`, formulário IA |
| 4 Biblioteca | Pesquisa, cards, PdfPreview (`#page` + zoom + hint) |
| 5 Design System | BaseCard, BaseButton, BaseBadge, Empty/Loading/Error, SearchInput |
| 6 Revisão | Migração Estatísticas, Login, Biblioteca, Simulados |

## 3. Ficheiros principais alterados (relativo ao ZIP original)

```
src/components/ui/*                    (novo)
src/assets/main.css                    (tokens + utilitários)
src/services/simuladoService.js
src/stores/simulado.js
src/views/FerramentasView.vue
src/views/FlashcardsView.vue
src/views/QuestoesView.vue
src/views/SimuladosView.vue
src/views/BibliotecaView.vue
src/views/ArtigoView.vue
src/views/DashboardView.vue
src/views/EstatisticasView.vue
src/views/LoginView.vue
src/components/biblioteca/PdfPreview.vue
src/components/biblioteca/ArticleActions.vue
```

Nota: não existem `diplomaService.js` / `artigoService.js` separados — a biblioteca usa **`bibliotecaService.js`** (diplomas, artigos, categorias, temas).

## 4. Como executar

Requisitos: Node 18+ (recomendado 20/22), backend em `http://localhost:8080`.

```bash
# extrair o ZIP
cd magistratura-frontend-final

npm install
npm run dev
```

Abre `http://localhost:5173` (proxy `/api` → `8080` via `vite.config.js`).

Build de produção:

```bash
npm run build
npm run preview
```

Login de teste (backend):

```
email: estudante@magistratura.local
password: 123456
```

## 5. Testes que deves executar

1. **Login** — JWT, redirecionamento dashboard  
2. **Biblioteca** — pesquisar, abrir diploma, lista de artigos  
3. **Artigo** — texto + PDF na página estimada (`paginaInicio`)  
4. **Flashcards** — Artigo → Gerar com IA → guardar → revisão  
5. **Questões** — Gerar → banco → responder → correção  
6. **Simulado** — criar com IA → listar → iniciar → finalizar  
7. **Tutor IA** — mensagem com contexto de diploma + streaming  
8. **PDF** — vários artigos (início / meio / fim do diploma)  
9. **Ollama off** — mensagem amigável (não stack técnica)

## 6. Notas

- Não foram adicionadas bibliotecas UI externas.
- Contratos de API inalterados.
- Build não foi executado no sandbox de entrega (rede limitada); valida com `npm run build` no Windows.

## 7. Após os teus testes

Envia os erros encontrados (mensagem, ecrã, passos) para a fase de correção pontual.

## Atualização — Módulo 7

Geração contextual directa no artigo (`ArtigoView` + `ArticleActions`).
Clicar «✨ Gerar flashcards/questões» chama `POST /api/ia/*` com `guardar:true`.
Ferramentas permanece como modo avançado.
