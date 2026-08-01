<script setup>
import { ref } from 'vue'
import { useSvgGsap } from '@/composables/useSvgGsap'

const props = defineProps({
  size: { type: [Number, String], default: 64 },
  animated: { type: Boolean, default: true }
})

const svgRef = ref(null)

useSvgGsap(
  svgRef,
  (gsap, root) => {
    const balance = root.querySelector('.bm-balance')
    const panL = root.querySelector('.bm-pan-l')
    const panR = root.querySelector('.bm-pan-r')
    const ring = root.querySelector('.bm-ring-gold')

    if (balance) {
      gsap.to(balance, {
        rotation: 2.4,
        transformOrigin: '40px 22px',
        duration: 2.6,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1
      })
    }
    if (panL) {
      gsap.to(panL, {
        y: 1.8,
        transformOrigin: '28px 22px',
        duration: 2.6,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1
      })
    }
    if (panR) {
      gsap.to(panR, {
        y: -1.8,
        transformOrigin: '52px 22px',
        duration: 2.6,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1
      })
    }
    if (ring) {
      gsap.fromTo(
        ring,
        { opacity: 0.4, attr: { 'stroke-width': 1 } },
        {
          opacity: 0.9,
          attr: { 'stroke-width': 1.5 },
          duration: 2.2,
          ease: 'sine.inOut',
          yoyo: true,
          repeat: -1
        }
      )
    }

    // entrada
    gsap.from(root, {
      scale: 0.88,
      opacity: 0,
      duration: 0.55,
      ease: 'back.out(1.4)'
    })
  },
  { enabled: props.animated }
)
</script>

<template>
  <svg
    ref="svgRef"
    class="brand-mark"
    :width="size"
    :height="size"
    viewBox="0 0 80 80"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <defs>
      <linearGradient id="bm-g" x1="12" y1="8" x2="68" y2="72" gradientUnits="userSpaceOnUse">
        <stop stop-color="#71904a" />
        <stop offset="1" stop-color="#56732f" />
      </linearGradient>
      <linearGradient id="bm-gold" x1="20" y1="20" x2="60" y2="60" gradientUnits="userSpaceOnUse">
        <stop stop-color="#e2c179" />
        <stop offset="1" stop-color="#bd8e3b" />
      </linearGradient>
    </defs>

    <circle class="bm-ring-outer" cx="40" cy="40" r="36" stroke="url(#bm-g)" stroke-width="2.5" fill="var(--color-surface, #fff)" />
    <circle class="bm-ring-gold" cx="40" cy="40" r="30" stroke="url(#bm-gold)" stroke-width="1" opacity="0.55" />

    <g class="bm-balance">
      <path d="M40 18v28" stroke="url(#bm-g)" stroke-width="2.2" stroke-linecap="round" />
      <path d="M28 22h24" stroke="url(#bm-g)" stroke-width="2" stroke-linecap="round" />
      <path class="bm-pan bm-pan-l" d="M28 22l-10 14h20L28 22z" fill="#e7edd5" stroke="#71904a" stroke-width="1.2" />
      <path class="bm-pan bm-pan-r" d="M52 22l-10 14h20L52 22z" fill="#e7edd5" stroke="#71904a" stroke-width="1.2" />
      <rect x="34" y="46" width="12" height="4" rx="1" fill="#56732f" />
      <path d="M26 54h28" stroke="#bd8e3b" stroke-width="2" stroke-linecap="round" />
    </g>
  </svg>
</template>

<style scoped>
.brand-mark {
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 4px 12px rgba(60, 45, 35, 0.12));
  will-change: transform;
}
</style>
