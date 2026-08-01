# Correção do Guia de Estudo — mensagens voltaram

## Causa
O `GuideHost` e a `policy` existiam, mas **quase ninguém emitia eventos**.
Só a jornada chamava `emitGuideEvent`. Por isso o balão nunca aparecia.

## O que foi feito
1. **Boas-vindas** ao entrar na app autenticada (`FIRST_LOGIN` / `RETURN_WITH_MEMORY`)
2. **Biblioteca** — dica ao entrar na rota + pesquisa sem resultados
3. **Questões** — mensagem após responder (acertou / errou)
4. **Artigo** — dica suave ao abrir um artigo
5. **Idle** — após ~4 min sem interacção
6. Cooldowns aumentados para não ser invasivo
7. Balão: `overflow: visible` + reposição após render

## Preferência
Em **Definições**, `guiaNivel` deve ser `normal` (não `desligado`).

## Ficheiros a copiar (sobrepor no projecto)

```
frontend/src/guide/events.js
frontend/src/guide/policy.js
frontend/src/layouts/AppLayout.vue
frontend/src/stores/questao.js
frontend/src/views/BibliotecaView.vue
frontend/src/components/guide/GuideHost.vue
```

Reinicia o frontend (`npm run dev`).  
Para testar de novo a boas-vindas nesta sessão do browser:

```js
sessionStorage.removeItem('magistratura.guide.sessionWelcome')
// opcional, primeira vez absoluta:
localStorage.removeItem('magistratura.guide.everWelcomed')
```

Depois recarrega a página autenticada.
