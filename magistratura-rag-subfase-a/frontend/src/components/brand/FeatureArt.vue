<script setup>
import { ref } from 'vue'
import { useSvgGsap } from '@/composables/useSvgGsap'

const props = defineProps({
  /** tutor | caminhada | biblioteca | questoes | flashcards | stats | simulados | settings | tools | mapa | perfil | default */
  variant: { type: String, default: 'default' },
  size: { type: [Number, String], default: 40 },
  animated: { type: Boolean, default: true }
})

const svgRef = ref(null)

useSvgGsap(
  svgRef,
  (gsap, root) => {
    const v = props.variant

    if (v === 'tutor') {
      const spark = root.querySelector('.fa-spark')
      const dots = root.querySelectorAll('.fa-dot')
      const bubble = root.querySelector('.fa-bubble')
      if (spark) {
        gsap.to(spark, {
          scale: 1.15,
          rotation: 12,
          transformOrigin: '36px 12.5px',
          duration: 1.4,
          ease: 'sine.inOut',
          yoyo: true,
          repeat: -1
        })
      }
      if (dots.length) {
        gsap.to(dots, {
          opacity: 0.25,
          duration: 0.35,
          stagger: 0.12,
          ease: 'power1.inOut',
          yoyo: true,
          repeat: -1,
          repeatDelay: 1.6
        })
      }
      if (bubble) {
        gsap.to(bubble, {
          scale: 1.03,
          transformOrigin: '20px 20px',
          duration: 2.2,
          ease: 'sine.inOut',
          yoyo: true,
          repeat: -1
        })
      }
    } else if (v === 'caminhada') {
      root.querySelectorAll('.fa-trail').forEach((el, i) => {
        const len = el.getTotalLength?.() || 48
        gsap.set(el, { strokeDasharray: len, strokeDashoffset: len })
        gsap.to(el, {
          strokeDashoffset: 0,
          duration: 1.5,
          delay: i * 0.2,
          ease: 'power2.out'
        })
      })
      const pageR = root.querySelector('.fa-page-r')
      if (pageR) {
        gsap.to(pageR, {
          rotation: 3,
          transformOrigin: '24px 12px',
          duration: 2.8,
          ease: 'sine.inOut',
          yoyo: true,
          repeat: -1
        })
      }
    } else if (v === 'biblioteca') {
      gsap.to(root.querySelectorAll('.fa-book'), {
        y: -2,
        duration: 1.8,
        stagger: 0.15,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1
      })
    } else {
      // generic idle for other variants
      const bob = root.querySelectorAll('.fa-bob')
      if (bob.length) {
        gsap.to(bob, {
          y: -1.5,
          duration: 2,
          stagger: 0.12,
          ease: 'sine.inOut',
          yoyo: true,
          repeat: -1
        })
      }
    }

    gsap.from(root, { opacity: 0, y: 5, duration: 0.4, ease: 'power2.out' })
  },
  { enabled: props.animated }
)
</script>

<template>
  <svg
    ref="svgRef"
    class="feature-art"
    :width="size"
    :height="size"
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <!-- Tutor -->
    <g v-if="variant === 'tutor'">
      <rect class="fa-bubble" x="6" y="10" width="28" height="20" rx="6" fill="#e7edd5" stroke="#71904a" stroke-width="1.6" />
      <path d="M14 30l-4 8 10-6" fill="#e7edd5" stroke="#71904a" stroke-width="1.6" stroke-linejoin="round" />
      <circle class="fa-dot" cx="16" cy="20" r="1.6" fill="#56732f" />
      <circle class="fa-dot" cx="22" cy="20" r="1.6" fill="#56732f" />
      <circle class="fa-dot" cx="28" cy="20" r="1.6" fill="#56732f" />
      <path class="fa-spark" d="M36 8l1.2 3.2L40.5 12.5l-3.3 1.2L36 17l-1.2-3.3L31.5 12.5l3.3-1.3L36 8z" fill="#bd8e3b" />
    </g>

    <!-- Caminhada / Experiências -->
    <g v-else-if="variant === 'caminhada'">
      <path class="fa-trail" d="M8 38c6-10 10-14 16-14s10 4 16 14" stroke="#b6c983" stroke-width="2.5" stroke-linecap="round" fill="none" />
      <path class="fa-trail" d="M14 28c4-6 7-8 10-8s6 2 10 8" stroke="#71904a" stroke-width="1.8" stroke-linecap="round" fill="none" opacity="0.7" />
      <path d="M24 8v18" stroke="#56732f" stroke-width="1.5" />
      <path d="M24 12c-6 1-10 4-10 8 0 0 4-2 10-2" fill="#e7edd5" stroke="#71904a" stroke-width="1.3" />
      <path class="fa-page-r" d="M24 12c6 1 10 4 10 8 0 0-4-2-10-2" fill="#f5ead0" stroke="#bd8e3b" stroke-width="1.3" />
    </g>

    <!-- Biblioteca / Documentos -->
    <g v-else-if="variant === 'biblioteca'">
      <rect class="fa-book" x="10" y="10" width="10" height="28" rx="1.5" fill="#e7edd5" stroke="#71904a" stroke-width="1.4" />
      <rect class="fa-book" x="22" y="14" width="9" height="24" rx="1.5" fill="#f5ead0" stroke="#bd8e3b" stroke-width="1.4" />
      <rect class="fa-book" x="33" y="12" width="8" height="26" rx="1.5" fill="#e7edd5" stroke="#56732f" stroke-width="1.4" />
    </g>

    <!-- Questões -->
    <g v-else-if="variant === 'questoes'">
      <rect class="fa-bob" x="10" y="10" width="28" height="28" rx="6" fill="#e7edd5" stroke="#71904a" stroke-width="1.5" />
      <circle cx="18" cy="20" r="2.2" fill="#56732f" />
      <path d="M24 18h10M24 24h8M24 30h6" stroke="#71904a" stroke-width="1.6" stroke-linecap="round" />
    </g>

    <!-- Flashcards -->
    <g v-else-if="variant === 'flashcards'">
      <rect class="fa-bob" x="12" y="14" width="22" height="26" rx="3" fill="#f5ead0" stroke="#bd8e3b" stroke-width="1.4" transform="rotate(-6 23 27)" />
      <rect class="fa-bob" x="14" y="10" width="22" height="26" rx="3" fill="#e7edd5" stroke="#71904a" stroke-width="1.5" />
      <path d="M20 20h10M20 26h8" stroke="#56732f" stroke-width="1.5" stroke-linecap="round" />
    </g>

    <!-- Estatísticas -->
    <g v-else-if="variant === 'stats'">
      <path d="M10 36h28" stroke="#71904a" stroke-width="1.5" stroke-linecap="round" />
      <rect class="fa-bob" x="12" y="22" width="6" height="14" rx="1.5" fill="#e7edd5" stroke="#71904a" stroke-width="1.2" />
      <rect class="fa-bob" x="21" y="14" width="6" height="22" rx="1.5" fill="#f5ead0" stroke="#bd8e3b" stroke-width="1.2" />
      <rect class="fa-bob" x="30" y="18" width="6" height="18" rx="1.5" fill="#e7edd5" stroke="#56732f" stroke-width="1.2" />
    </g>

    <!-- Simulados -->
    <g v-else-if="variant === 'simulados'">
      <circle class="fa-bob" cx="24" cy="24" r="14" fill="#e7edd5" stroke="#71904a" stroke-width="1.6" />
      <path d="M24 16v9l6 3" stroke="#56732f" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      <circle cx="24" cy="24" r="2" fill="#bd8e3b" />
    </g>

    <!-- Definições -->
    <g v-else-if="variant === 'settings'">
      <circle class="fa-bob" cx="24" cy="24" r="8" fill="#e7edd5" stroke="#71904a" stroke-width="1.5" />
      <circle cx="24" cy="24" r="3" fill="#56732f" />
      <path
        d="M24 10v4M24 34v4M10 24h4M34 24h4M13.5 13.5l2.8 2.8M31.7 31.7l2.8 2.8M13.5 34.5l2.8-2.8M31.7 16.3l2.8-2.8"
        stroke="#71904a"
        stroke-width="1.6"
        stroke-linecap="round"
      />
    </g>

    <!-- Ferramentas -->
    <g v-else-if="variant === 'tools'">
      <path class="fa-bob" d="M16 32l12-12 4 4-12 12-5 1 1-5z" fill="#e7edd5" stroke="#71904a" stroke-width="1.4" />
      <path d="M30 14l4 4" stroke="#bd8e3b" stroke-width="2" stroke-linecap="round" />
      <circle cx="34" cy="14" r="3" fill="#f5ead0" stroke="#bd8e3b" stroke-width="1.3" />
    </g>

    <!-- Mapa jurídico -->
    <g v-else-if="variant === 'mapa'">
      <circle class="fa-bob" cx="16" cy="18" r="4" fill="#e7edd5" stroke="#71904a" stroke-width="1.3" />
      <circle class="fa-bob" cx="32" cy="16" r="4" fill="#f5ead0" stroke="#bd8e3b" stroke-width="1.3" />
      <circle class="fa-bob" cx="24" cy="32" r="4" fill="#e7edd5" stroke="#56732f" stroke-width="1.3" />
      <path d="M19 20l9-2M18 21l5 9M30 19l-5 10" stroke="#71904a" stroke-width="1.4" stroke-linecap="round" />
    </g>

    <!-- Perfil -->
    <g v-else-if="variant === 'perfil'">
      <circle class="fa-bob" cx="24" cy="18" r="7" fill="#f6dad0" stroke="#71904a" stroke-width="1.4" />
      <ellipse cx="24" cy="36" rx="12" ry="8" fill="#e7edd5" stroke="#71904a" stroke-width="1.4" />
    </g>

    <!-- Default -->
    <g v-else>
      <circle class="fa-bob" cx="24" cy="24" r="16" stroke="#71904a" stroke-width="2" fill="#e7edd5" />
      <path d="M24 14v20M16 24h16" stroke="#56732f" stroke-width="2" stroke-linecap="round" />
    </g>
  </svg>
</template>

<style scoped>
.feature-art {
  display: block;
  flex-shrink: 0;
  will-change: transform, opacity;
}
</style>
