# Capítulos + Experiência

## Correcção login
Um patch anterior usava `auth.estaAutenticado` em vez de `auth.autenticado`,
o que partia o `beforeEach` do router. O `router/index.js` deste zip está
restaurado com a lógica original + a rota da experiência.

## Rotas
- `/caminhada` — capítulos
- `/caminhada/:momentoId` — experiência (ex.: `/caminhada/e1`)

## Se o teu router tiver mais rotas (simulados, admin…)
Não substituas o ficheiro inteiro às cegas: copia só o bloco:

```js
{
  path: 'caminhada/:momentoId',
  name: 'experiencia-joao',
  component: () => import('@/views/ExperienciaJoaoView.vue')
}
```

e confirma que o guard usa `auth.autenticado`.
