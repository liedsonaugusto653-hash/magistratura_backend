<script setup>
/**
 * Guia de estudo — arrastável; o balão mantém-se sempre dentro do viewport.
 */
import { onMounted, onUnmounted, computed, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useGuideStore } from '@/stores/guide'
import { Compass, Volume2, VolumeX, X } from 'lucide-vue-next'

const LS_POS = 'magistratura.guide.pos'
const AVATAR = 52
const MARGIN = 12
const GAP = 10

const guide = useGuideStore()
const { modo, mensagem, vozActiva } = storeToRefs(guide)
const router = useRouter()

const hostEl = ref(null)
const balaoEl = ref(null)
const pos = ref({ x: null, y: null })
const aArrastar = ref(false)
const dragMoved = ref(false)
/** 'acima' | 'abaixo' — lado do balão relativamente ao avatar */
const ladoVertical = ref('acima')
/** 'esquerda' | 'direita' — alinhamento horizontal do balão */
const ladoHorizontal = ref('direita')

let dragOffsetX = 0
let dragOffsetY = 0
let utterActual = null

onMounted(() => {
  guide.iniciar()
  carregarPosicao()
  window.addEventListener('resize', emResize)
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.getVoices()
  }
})

onUnmounted(() => {
  guide.parar()
  window.removeEventListener('resize', emResize)
  soltarListeners()
  pararVoz()
})

const mostrar = computed(() => modo.value === 'fala' || modo.value === 'alerta')

const estiloHost = computed(() => {
  if (pos.value.x == null || pos.value.y == null) {
    return { right: '1.25rem', bottom: '5.5rem', left: 'auto', top: 'auto' }
  }
  return {
    left: `${pos.value.x}px`,
    top: `${pos.value.y}px`,
    right: 'auto',
    bottom: 'auto'
  }
})

const classeBalao = computed(() => ({
  alerta: modo.value === 'alerta',
  'v-acima': ladoVertical.value === 'acima',
  'v-abaixo': ladoVertical.value === 'abaixo',
  'h-direita': ladoHorizontal.value === 'direita',
  'h-esquerda': ladoHorizontal.value === 'esquerda'
}))

watch(
  () => mensagem.value?.text,
  async (texto) => {
    pararVoz()
    if (texto && vozActiva.value && mostrar.value) falar(texto)
    if (texto && mostrar.value) {
      await nextTick()
      await nextTick()
      requestAnimationFrame(() => garantirVisivel())
    }
  }
)

watch(vozActiva, (on) => {
  if (!on) pararVoz()
  else if (mensagem.value?.text && mostrar.value) falar(mensagem.value.text)
})

watch(mostrar, async (v) => {
  if (v) {
    await nextTick()
    garantirVisivel()
  }
})

function falar(texto) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  pararVoz()
  const u = new SpeechSynthesisUtterance(texto)
  u.lang = 'pt-PT'
  u.rate = 1
  const vozes = window.speechSynthesis.getVoices() || []
  const pt =
    vozes.find((v) => v.lang === 'pt-PT') ||
    vozes.find((v) => v.lang?.startsWith('pt')) ||
    null
  if (pt) u.voice = pt
  utterActual = u
  window.speechSynthesis.speak(u)
}

function pararVoz() {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
  utterActual = null
}

function carregarPosicao() {
  try {
    const raw = localStorage.getItem(LS_POS)
    if (!raw) return
    const p = JSON.parse(raw)
    if (typeof p.x === 'number' && typeof p.y === 'number') {
      pos.value = limitarAvatar(p.x, p.y)
    }
  } catch {
    /* ignore */
  }
}

function gravarPosicao() {
  try {
    if (pos.value.x != null && pos.value.y != null) {
      localStorage.setItem(LS_POS, JSON.stringify(pos.value))
    }
  } catch {
    /* ignore */
  }
}

function limitarAvatar(x, y) {
  const maxX = Math.max(MARGIN, window.innerWidth - AVATAR - MARGIN)
  const maxY = Math.max(MARGIN, window.innerHeight - AVATAR - MARGIN)
  return {
    x: Math.min(maxX, Math.max(MARGIN, x)),
    y: Math.min(maxY, Math.max(MARGIN, y))
  }
}

/**
 * Escolhe lado do balão e, se preciso, desloca o avatar
 * para que o conjunto fique dentro do ecrã.
 */
function garantirVisivel() {
  const balao = balaoEl.value
  if (!balao || typeof window === 'undefined') return

  // Coordenadas actuais do avatar
  let ax
  let ay
  if (pos.value.x != null && pos.value.y != null) {
    ax = pos.value.x
    ay = pos.value.y
  } else {
    const host = hostEl.value
    if (!host) return
    const r = host.getBoundingClientRect()
    // avatar está no fundo do host
    ax = r.right - AVATAR
    ay = r.bottom - AVATAR
    pos.value = limitarAvatar(ax, ay)
    ax = pos.value.x
    ay = pos.value.y
  }

  const bw = Math.min(320, window.innerWidth - 2 * MARGIN)
  // altura estimada / medida
  const bh = balao.offsetHeight || 160

  const spaceAbove = ay - MARGIN
  const spaceBelow = window.innerHeight - (ay + AVATAR) - MARGIN
  if (spaceAbove >= bh + GAP || spaceAbove >= spaceBelow) {
    ladoVertical.value = 'acima'
  } else {
    ladoVertical.value = 'abaixo'
  }

  // Preferir alinhar à direita do avatar (balão termina no bordo direito do avatar)
  const preferRight = ax + AVATAR <= window.innerWidth - MARGIN
  if (preferRight && ax + AVATAR - bw >= MARGIN) {
    ladoHorizontal.value = 'direita'
  } else if (ax + bw <= window.innerWidth - MARGIN) {
    ladoHorizontal.value = 'esquerda'
  } else {
    // Centrar o melhor possível deslocando o avatar
    ladoHorizontal.value = 'direita'
    const targetRight = Math.min(window.innerWidth - MARGIN, Math.max(MARGIN + bw, ax + AVATAR))
    ax = targetRight - AVATAR
  }

  // Ajuste vertical se ainda não cabe
  if (ladoVertical.value === 'acima') {
    const topBalao = ay - GAP - bh
    if (topBalao < MARGIN) {
      ay = MARGIN + bh + GAP
      // se assim o avatar sai por baixo, põe balão abaixo
      if (ay + AVATAR > window.innerHeight - MARGIN) {
        ladoVertical.value = 'abaixo'
        ay = Math.min(ay, window.innerHeight - AVATAR - MARGIN)
        if (ay + AVATAR + GAP + bh > window.innerHeight - MARGIN) {
          ay = Math.max(MARGIN, window.innerHeight - MARGIN - AVATAR - GAP - Math.min(bh, 200))
        }
      }
    }
  } else {
    const bottomBalao = ay + AVATAR + GAP + bh
    if (bottomBalao > window.innerHeight - MARGIN) {
      ay = Math.max(MARGIN, window.innerHeight - MARGIN - AVATAR - GAP - bh)
      if (ay < MARGIN + 40) {
        ladoVertical.value = 'acima'
        ay = Math.min(window.innerHeight - AVATAR - MARGIN, MARGIN + bh + GAP)
      }
    }
  }

  pos.value = limitarAvatar(ax, ay)
  gravarPosicao()
}

function emResize() {
  if (pos.value.x == null) return
  pos.value = limitarAvatar(pos.value.x, pos.value.y)
  if (mostrar.value) nextTick(() => garantirVisivel())
  else gravarPosicao()
}

function onPointerDown(e) {
  if (e.button != null && e.button !== 0) return
  // Permitir arrastar a partir do avatar ou da barra do balão
  const host = hostEl.value
  if (!host) return

  if (pos.value.x == null || pos.value.y == null) {
    const rect = host.getBoundingClientRect()
    pos.value = limitarAvatar(rect.right - AVATAR, rect.bottom - AVATAR)
  }

  dragOffsetX = e.clientX - pos.value.x
  dragOffsetY = e.clientY - pos.value.y
  aArrastar.value = true
  dragMoved.value = false
  e.currentTarget.setPointerCapture?.(e.pointerId)
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('pointercancel', onPointerUp)
  e.preventDefault()
}

function onPointerMove(e) {
  if (!aArrastar.value) return
  dragMoved.value = true
  pos.value = limitarAvatar(e.clientX - dragOffsetX, e.clientY - dragOffsetY)
  if (mostrar.value) {
    // reavalia lado enquanto arrasta (leve)
    const ay = pos.value.y
    const bh = balaoEl.value?.offsetHeight || 160
    const spaceAbove = ay - MARGIN
    const spaceBelow = window.innerHeight - (ay + AVATAR) - MARGIN
    ladoVertical.value =
      spaceAbove >= bh + GAP || spaceAbove >= spaceBelow ? 'acima' : 'abaixo'
    const ax = pos.value.x
    const bw = Math.min(320, window.innerWidth - 2 * MARGIN)
    ladoHorizontal.value =
      ax + AVATAR - bw >= MARGIN ? 'direita' : 'esquerda'
  }
}

function onPointerUp() {
  if (!aArrastar.value) return
  aArrastar.value = false
  if (mostrar.value) garantirVisivel()
  else gravarPosicao()
  soltarListeners()
  setTimeout(() => {
    dragMoved.value = false
  }, 40)
}

function soltarListeners() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
}

function onAvatarClick() {
  if (dragMoved.value) return
  if (mostrar.value) dispensar()
}

function onAction(a) {
  if (a.to) {
    pararVoz()
    router.push(a.to)
  }
  if (a.dismiss !== false) dispensar()
}

function dispensar() {
  pararVoz()
  guide.dispensar()
}

function toggleVoz() {
  guide.alternarVoz()
}
</script>

<template>
  <div ref="hostEl" class="guide-host" :style="estiloHost" aria-live="polite">
    <transition name="guide-pop">
      <div
        v-if="mostrar && mensagem"
        ref="balaoEl"
        class="balao"
        :class="classeBalao"
      >
        <div
          class="balao-topo"
          title="Arrasta para mover o guia"
          @pointerdown="onPointerDown"
        >
          <span class="balao-titulo">Guia de estudo</span>
          <div class="balao-acoes-icon" @pointerdown.stop>
            <button
              type="button"
              class="icon-btn"
              :title="vozActiva ? 'Desligar voz (apenas ler)' : 'Ligar voz'"
              @click="toggleVoz"
            >
              <Volume2 v-if="vozActiva" :size="15" />
              <VolumeX v-else :size="15" />
            </button>
            <button type="button" class="icon-btn" title="Fechar" @click="dispensar">
              <X :size="15" />
            </button>
          </div>
        </div>
        <div class="balao-corpo">
          <p class="balao-texto">{{ mensagem.text }}</p>
          <p v-if="vozActiva" class="balao-hint">A ler em voz alta · podes desligar o som</p>
          <p v-else class="balao-hint">Apenas texto · toca no ícone para ouvir</p>
          <div v-if="mensagem.actions?.length" class="balao-actions">
            <button
              v-for="a in mensagem.actions"
              :key="a.id"
              type="button"
              class="btn-action"
              :class="{ primario: !a.dismiss || a.to }"
              @click="onAction(a)"
            >
              {{ a.label }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <button
      type="button"
      class="avatar"
      :class="{ activo: mostrar, arrastar: aArrastar }"
      title="Guia de estudo (arrasta para mover)"
      @pointerdown="onPointerDown"
      @click="onAvatarClick"
    >
      <Compass :size="22" />
    </button>
  </div>
</template>

<style scoped>
.guide-host {
  position: fixed;
  z-index: 90;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0;
  pointer-events: none;
  width: 52px;
  height: 52px;
  overflow: visible;
}
.guide-host > * {
  pointer-events: auto;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 2px solid var(--color-secondary-300, #9bb89a);
  background: linear-gradient(
    145deg,
    var(--color-secondary-100, #e8f2eb),
    var(--color-surface, #fff)
  );
  color: var(--color-secondary-700, #2d4a2c);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 4px 14px rgba(30, 50, 40, 0.12);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}
.avatar:hover {
  transform: scale(1.04);
}
.avatar.activo {
  border-color: var(--color-secondary-500);
  box-shadow: 0 4px 18px rgba(61, 107, 79, 0.22);
}
.avatar.arrastar {
  cursor: grabbing;
  transform: scale(1.06);
}

.balao {
  position: absolute;
  width: min(320px, calc(100vw - 24px));
  max-height: min(360px, calc(100vh - 80px));
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-secondary-200, #c5ddce);
  border-radius: 16px;
  box-shadow: 0 8px 28px rgba(30, 50, 40, 0.12);
  z-index: 1;
  overflow: hidden;
}
.balao.alerta {
  border-color: #e8b4a8;
}
/* Posição relativa ao avatar (canto inferior direito do host = avatar) */
.balao.v-acima {
  bottom: calc(52px + 10px);
}
.balao.v-abaixo {
  top: calc(52px + 10px);
}
.balao.h-direita {
  right: 0;
}
.balao.h-esquerda {
  left: 0;
  right: auto;
}

.balao-topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.55rem 0.75rem;
  background: var(--color-secondary-50, #f4faf6);
  border-bottom: 1px solid var(--color-secondary-100, #e8f2eb);
  cursor: grab;
  flex-shrink: 0;
  user-select: none;
  touch-action: none;
}
.balao-titulo {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-secondary-600);
}
.balao-acoes-icon {
  display: flex;
  gap: 0.2rem;
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
}
.icon-btn:hover {
  background: var(--color-secondary-100, #e8f2eb);
  color: var(--color-secondary-700);
}

.balao-corpo {
  padding: 0.75rem 0.9rem 0.9rem;
  overflow-y: auto;
  overscroll-behavior: contain;
}
.balao-texto {
  margin: 0 0 0.35rem;
  font-size: 0.9rem;
  line-height: 1.45;
  color: var(--color-text);
}
.balao-hint {
  margin: 0 0 0.65rem;
  font-size: 0.72rem;
  color: var(--color-text-muted);
}
.balao-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}
.btn-action {
  font-family: inherit;
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
}
.btn-action.primario {
  background: var(--color-secondary-100, #e8f2eb);
  border-color: var(--color-secondary-300);
  color: var(--color-secondary-800);
}
.btn-action:hover {
  border-color: var(--color-secondary-400);
}

.guide-pop-enter-active,
.guide-pop-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.guide-pop-enter-from,
.guide-pop-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.96);
}
</style>
