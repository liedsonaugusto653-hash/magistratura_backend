# Magistrado Angola — Camada de Experiência (Guia + Caminhada)

## O que isto é

Não é “mais um módulo de estudo”. É a camada que faz o sistema **acompanhar** o utilizador:

1. **Guia de Estudo** — presença fixa no layout, orientada a **eventos** (não é IA, não é chatbot).
2. **Caminhada («Com o João»)** — um único fio no menu; narrativa que desagua na Biblioteca e no Tutor.

A Biblioteca, o Tutor, os PDFs, flashcards e o resto **permanecem soberanos**.

## Filosofia (medida de sucesso)

> «Entrei para acompanhar a história do João… e um dia percebi que já conseguia ler uma lei sozinho.»

Se a UI parecer LMS, curso ou mascote decorativa — falhou.

## Como aplicar

Na raiz do monorepo (`backend/` e `frontend/`):

```bash
# Frontend — copiar ficheiros novos e substituições
cp -R experiencia/frontend/src/guide frontend/src/
cp -R experiencia/frontend/src/jornada frontend/src/
cp experiencia/frontend/src/stores/guide.js frontend/src/stores/
cp experiencia/frontend/src/stores/jornada.js frontend/src/stores/
cp experiencia/frontend/src/services/jornadaService.js frontend/src/services/
cp experiencia/frontend/src/components/guide/GuideHost.vue frontend/src/components/guide/
mkdir -p frontend/src/components/guide
cp experiencia/frontend/src/views/CaminhadaView.vue frontend/src/views/
cp experiencia/frontend/src/layouts/AppLayout.vue frontend/src/layouts/
cp experiencia/frontend/src/router/index.js frontend/src/router/
cp experiencia/frontend/src/config/navIcons.js frontend/src/config/
cp experiencia/frontend/src/views/DashboardView.vue frontend/src/views/
cp experiencia/frontend/src/views/DefinicoesView.vue frontend/src/views/

# Backend — só acrescento
cp experiencia/backend/src/main/resources/db/migration/V29__jornada_progresso.sql \
   backend/src/main/resources/db/migration/
mkdir -p backend/src/main/java/ao/magistratura/jornada/{service,web}
cp experiencia/backend/src/main/java/ao/magistratura/jornada/service/JornadaService.java \
   backend/src/main/java/ao/magistratura/jornada/service/
cp experiencia/backend/src/main/java/ao/magistratura/jornada/web/JornadaController.java \
   backend/src/main/java/ao/magistratura/jornada/web/
```

Reiniciar backend (Flyway **V29**). Frontend: `npm run dev`.

## O que muda na navegação

- **+1 item**: «Com o João» (ordem 15, entre Painel e Tutor).
- **Guia**: avatar no canto inferior direito em todas as rotas autenticadas.
- **Dashboard**: se houver gancho narrativo, a saudação vira continuidade da história.
- **Definições**: intensidade do Guia (`normal` | `minimo` | `desligado`).

## API nova (mínima)

| Método | Path | Função |
|--------|------|--------|
| GET | `/api/jornada/progresso` | momento/cena/concluidos |
| PUT | `/api/jornada/progresso` | gravar progresso |
| GET | `/api/jornada/disponibilidade` | se há diplomas com artigos |

Sem endpoints de “lições”. Conteúdo narrativo = seed no frontend (`jornada/seed.js`).

## Emitir eventos do Guia (noutros ecrãs)

```js
import { emitGuideEvent, GuideEvent } from '@/stores/guide'

// PDF a processar
emitGuideEvent(GuideEvent.DOCUMENT_PROCESSING, { mensagem: '…' })

// Pesquisa vazia
emitGuideEvent(GuideEvent.EMPTY_SEARCH)

// Artigo com cena ligada (fase seguinte)
emitGuideEvent(GuideEvent.ARTICLE_HAS_SCENE, { to: '/caminhada', viu: false })
```

## Próximos passos (não bloqueantes)

1. Fio discreto no `ArtigoView` quando existir âncora cena↔artigoId.
2. Preferência `guiaNivel` já lida pelo store do Guia via `auth.preferencias()`.
3. Mais momentos no seed à medida que a biblioteca cresce (sempre com problema vivido pelo João primeiro).
4. Consequências de UI silenciosas (ex.: realce de estrutura de artigo após m2).

## O que explicitamente não fazer

- Menu “Aprendizagem / Curso / Lições / Guia”.
- Transformar o Guia num segundo chat.
- Inventar diplomas na narrativa que não existem na BD.
- Copy do tipo “hoje vais aprender…”.
