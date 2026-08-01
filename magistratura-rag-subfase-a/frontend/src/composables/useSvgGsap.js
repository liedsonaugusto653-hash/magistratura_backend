import { onMounted, onUnmounted, unref } from 'vue'

/**
 * Anima SVGs com GSAP. Respeita prefers-reduced-motion e limpa no unmount.
 * Requer: npm install gsap
 */
export function useSvgGsap(rootRef, build, opts = {}) {
  let ctx = null

  onMounted(async () => {
    const enabled = opts.enabled === undefined ? true : unref(opts.enabled)
    if (!enabled) return

    if (
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      return
    }

    const root = unref(rootRef)
    if (!root) return

    try {
      // @vite-ignore — evita falha de analysis se o pacote ainda não estiver instalado
      const mod = await import(/* @vite-ignore */ 'gsap')
      const gsap = mod.default || mod.gsap || mod
      if (!gsap?.context) {
        console.warn('[useSvgGsap] GSAP carregado mas sem API context. Corre: npm install gsap')
        return
      }
      ctx = gsap.context(() => {
        build(gsap, root)
      }, root)
    } catch (e) {
      console.warn(
        '[useSvgGsap] GSAP não encontrado. Na pasta frontend corre:\n  npm install gsap\n',
        e?.message || e
      )
    }
  })

  onUnmounted(() => {
    if (ctx) {
      ctx.revert()
      ctx = null
    }
  })
}
