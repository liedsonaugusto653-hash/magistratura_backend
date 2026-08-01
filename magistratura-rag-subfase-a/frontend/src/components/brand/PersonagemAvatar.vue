<script setup>
import { ref } from 'vue'
import { useSvgGsap } from '@/composables/useSvgGsap'

const props = defineProps({
  personagemId: { type: String, default: 'joao' },
  size: { type: [Number, String], default: 40 },
  animated: { type: Boolean, default: true }
})

const svgRef = ref(null)

useSvgGsap(
  svgRef,
  (gsap, root) => {
    const eyes = root.querySelector('.av-eyes')
    const ring = root.querySelector('.av-ring')
    const smile = root.querySelector('.av-smile')

    if (eyes) {
      // piscar periódico
      const tl = gsap.timeline({ repeat: -1, repeatDelay: 3.2 })
      tl.to(eyes, {
        scaleY: 0.12,
        transformOrigin: '24px 26px',
        duration: 0.08,
        ease: 'power1.in'
      }).to(eyes, {
        scaleY: 1,
        duration: 0.1,
        ease: 'power1.out'
      })
    }

    if (ring) {
      gsap.to(ring, {
        attr: { stroke: '#9bb86a' },
        duration: 2.5,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1
      })
    }

    if (smile) {
      gsap.to(smile, {
        opacity: 1,
        duration: 2,
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1,
        from: { opacity: 0.7 }
      })
    }

    gsap.from(root, {
      scale: 0.9,
      opacity: 0,
      duration: 0.45,
      ease: 'back.out(1.6)'
    })
  },
  { enabled: props.animated }
)
</script>

<template>
  <svg
    ref="svgRef"
    class="avatar"
    :width="size"
    :height="size"
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <circle class="av-ring" cx="24" cy="24" r="22" fill="#e7edd5" stroke="#71904a" stroke-width="1.5" />
    <g v-if="personagemId === 'joao' || !personagemId" class="av-joao">
      <ellipse cx="24" cy="28" rx="11" ry="12" fill="#f6dad0" />
      <path d="M14 22c2-8 16-8 18 0" fill="#3f5622" />
      <g class="av-eyes">
        <circle class="av-eye" cx="19" cy="26" r="1.4" fill="#10160f" />
        <circle class="av-eye" cx="29" cy="26" r="1.4" fill="#10160f" />
      </g>
      <path class="av-smile" d="M20 32c2 1.5 6 1.5 8 0" stroke="#8c4e35" stroke-width="1.2" stroke-linecap="round" fill="none" />
    </g>
    <g v-else>
      <circle cx="24" cy="22" r="8" fill="#f6dad0" />
      <ellipse cx="24" cy="38" rx="12" ry="8" fill="#71904a" />
    </g>
  </svg>
</template>

<style scoped>
.avatar {
  display: block;
  border-radius: 50%;
  flex-shrink: 0;
  will-change: transform;
}
</style>
