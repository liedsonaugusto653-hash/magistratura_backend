<script setup>
/**
 * Leitura em voz alta de texto jurídico (Web Speech API).
 * Reutilizável em artigos, cartões e experiências.
 */
import { ref, watch, onUnmounted, computed } from 'vue'
import { Volume2, Square, Pause, Play, SkipBack, SkipForward } from 'lucide-vue-next'
import {
  partirEmFrases,
  criarNarrador,
  suporteTts,
  quandoVozesProntas,
  assinaturaBlocos
} from '@/jornada/tts'

const props = defineProps({
  /** Texto a ler (oficial / jurídico) */
  texto: { type: String, default: '' },
  /** Rótulo do botão */
  label: { type: String, default: 'Ouvir texto' },
  /** Mostrar texto com destaque sincronizado */
  comDestaque: { type: Boolean, default: true },
  /** Variante visual compacta (só botão, sem bloco de texto) */
  compacto: { type: Boolean, default: false }
})

const disponivel = suporteTts()
const aOuvir = ref(false)
const pausado = ref(false)
const indiceActivo = ref(-1)
const velocidade = ref(1)
const erro = ref('')

let narrador = null

const frases = computed(() => partirEmFrases(props.texto))
const segmentos = computed(() =>
  frases.value.map((texto, i) => ({
    key: `t-${i}`,
    tipo: 'narrativa',
    texto
  }))
)
const total = computed(() => segmentos.value.length)
const progresso = computed(() => {
  if (!total.value) return ''
  if (indiceActivo.value < 0) return aOuvir.value ? `1/${total.value}` : `0/${total.value}`
  return `${indiceActivo.value + 1}/${total.value}`
})

function destruir() {
  narrador?.parar()
  narrador = null
}

function criar() {
  destruir()
  if (!segmentos.value.length) return null
  return criarNarrador(segmentos.value, {
    rate: velocidade.value,
    onIndex(i) {
      indiceActivo.value = i
    },
    onFim() {
      aOuvir.value = false
      pausado.value = false
      indiceActivo.value = -1
      narrador = null
    },
    onErro() {
      aOuvir.value = false
      pausado.value = false
      erro.value = 'Não foi possível iniciar a leitura.'
      narrador = null
    }
  })
}

watch(
  () => props.texto,
  () => {
    destruir()
    aOuvir.value = false
    pausado.value = false
    indiceActivo.value = -1
    erro.value = ''
  }
)

onUnmounted(() => destruir())

async function iniciar(desde = 0) {
  erro.value = ''
  if (!disponivel) {
    erro.value = 'Leitura em voz alta indisponível neste browser.'
    return
  }
  if (!segmentos.value.length) return
  await quandoVozesProntas()
  narrador = criar()
  if (!narrador) return
  aOuvir.value = true
  pausado.value = false
  narrador.iniciar(desde)
}

function parar() {
  destruir()
  aOuvir.value = false
  pausado.value = false
  indiceActivo.value = -1
}

function alternarPausa() {
  if (!narrador || !aOuvir.value) return
  if (pausado.value) {
    narrador.retomar()
    pausado.value = false
  } else {
    narrador.pausar()
    pausado.value = true
  }
}

function anterior() {
  if (!total.value) return
  if (!narrador || !aOuvir.value) {
    iniciar(Math.max(0, (indiceActivo.value > 0 ? indiceActivo.value : 1) - 1))
    return
  }
  pausado.value = false
  narrador.anterior()
}

function seguinte() {
  if (!total.value) return
  if (!narrador || !aOuvir.value) {
    const i =
      indiceActivo.value >= 0
        ? Math.min(indiceActivo.value + 1, total.value - 1)
        : 0
    iniciar(i)
    return
  }
  pausado.value = false
  narrador.seguinte()
}

function mudarVelocidade(v) {
  velocidade.value = v
  if (aOuvir.value && narrador) {
    const i = Math.max(0, indiceActivo.value)
    narrador.setRate(v)
    narrador.irPara(i)
  }
}
</script>

<template>
  <div class="ouvir-texto" :class="{ compacto }">
    <div class="controlos">
      <button
        v-if="!aOuvir"
        type="button"
        class="btn-ouvir"
        :disabled="!disponivel || !texto?.trim()"
        @click="iniciar(0)"
      >
        <Volume2 :size="15" />
        {{ label }}
      </button>
      <template v-else>
        <button type="button" class="btn-ouvir activo" @click="alternarPausa">
          <Pause v-if="!pausado" :size="15" />
          <Play v-else :size="15" />
          {{ pausado ? 'Continuar' : 'Pausar' }}
        </button>
        <button type="button" class="btn-icon" title="Parar" @click="parar">
          <Square :size="13" />
        </button>
      </template>

      <div class="nav-frase">
        <button type="button" class="btn-icon" title="Frase anterior" :disabled="!total" @click="anterior">
          <SkipBack :size="15" />
        </button>
        <span v-if="total" class="progresso">{{ progresso }}</span>
        <button type="button" class="btn-icon" title="Frase seguinte" :disabled="!total" @click="seguinte">
          <SkipForward :size="15" />
        </button>
      </div>

      <div class="velocidades">
        <button
          v-for="v in [0.75, 1, 1.25]"
          :key="v"
          type="button"
          class="vel-btn"
          :class="{ activo: velocidade === v }"
          @click="mudarVelocidade(v)"
        >
          {{ v }}x
        </button>
      </div>
    </div>

    <p v-if="erro" class="erro">{{ erro }}</p>

    <div v-if="comDestaque && !compacto && frases.length" class="texto-destaque" aria-live="polite">
      <span
        v-for="(frase, i) in frases"
        :key="'f-' + i"
        class="frase"
        :class="{ activa: indiceActivo === i }"
        role="button"
        tabindex="0"
        @click="iniciar(i)"
        @keydown.enter="iniciar(i)"
      >{{ frase }}&#32;</span>
    </div>
  </div>
</template>

<style scoped>
.ouvir-texto {
  margin-bottom: 0.75rem;
}
.controlos {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.65rem;
}
.btn-ouvir {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-family: inherit;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.4rem 0.85rem;
  border-radius: 999px;
  border: 1.5px solid var(--color-secondary-300, #9bb89a);
  background: var(--color-secondary-100, #eef6f2);
  color: var(--color-secondary-700, #2d4a2c);
  cursor: pointer;
}
.btn-ouvir:hover:not(:disabled) {
  border-color: var(--color-secondary-500);
}
.btn-ouvir:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-ouvir.activo {
  background: var(--color-secondary-500, #4a7c59);
  border-color: var(--color-secondary-500, #4a7c59);
  color: #fff;
}
.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-surface, #fff);
  color: var(--color-text-muted);
  cursor: pointer;
}
.btn-icon:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.nav-frase {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
}
.progresso {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-text-muted);
  min-width: 2.4rem;
  text-align: center;
}
.velocidades {
  display: inline-flex;
  gap: 0.15rem;
  margin-left: auto;
}
.vel-btn {
  font-family: inherit;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.22rem 0.4rem;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
}
.vel-btn.activo {
  border-color: var(--color-secondary-400);
  color: var(--color-secondary-700);
  background: var(--color-secondary-100, #eef6f2);
}
.erro {
  margin: 0 0 0.5rem;
  font-size: 0.8rem;
  color: var(--color-danger, #b33);
}
.texto-destaque {
  font-size: 1rem;
  line-height: 1.7;
  color: var(--color-text);
  white-space: pre-wrap;
}
.frase {
  border-radius: 3px;
  cursor: pointer;
  transition: background 0.15s ease;
}
.frase:hover {
  background: var(--color-secondary-50, #f3faf5);
}
.frase.activa {
  background: var(--color-secondary-100, #e8f5e9);
  color: var(--color-secondary-900, #1a2e19);
}
.compacto .controlos {
  margin-bottom: 0;
}
</style>
