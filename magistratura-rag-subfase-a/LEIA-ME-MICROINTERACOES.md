# Microinterações visuais — patch frontend

## Aplicar
Na raiz do projecto (pasta com `frontend/`):

```bash
unzip -o magistratura-microinteracoes-front.zip
```

Reinicia `npm run dev`.

## O que muda
- Tokens `--motion-*` + `prefers-reduced-motion`
- Transição de página (router-view fade/slide)
- Stagger nos blocos da narrativa
- Cartões Caminhada: hover elevação 2px
- Segmento TTS activo com underline inset
- CTAs da experiência com hover/active
- `:focus-visible` global
- Skeleton utilitário `.mi-skeleton`

Filosofia: fluidez sem gamificação nem celebração de “lição aprendida”.
