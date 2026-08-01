# Leitura em voz alta — texto jurídico

## Onde
- **Artigo (Biblioteca)** — cartão «Texto oficial» (`ArticleContent.vue`)
  - Botão «Ouvir texto oficial»
  - Destaque da frase activa
  - Pausar / parar / avançar / recuar / 0.75x · 1x · 1.25x

## Componentes
- `components/audio/OuvirTexto.vue` — reutilizável em qualquer cartão com texto
- `components/biblioteca/ArticleContent.vue` — integração no texto oficial
- `jornada/tts.js` — motor Web Speech (partilhado com a experiência do João)

## Usar noutro sítio
```vue
<OuvirTexto :texto="algumTextoJuridico" label="Ouvir" />
<!-- só controlos, sem re-render do texto: -->
<OuvirTexto :texto="trecho" label="Ouvir" compacto :com-destaque="false" />
```
