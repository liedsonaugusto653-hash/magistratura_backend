<script setup>
/**
 * Ouvir a experiência do João — imersão, não aula.
 * Lê a história até ao fim; reflexão só depois.
 * Avançar / recuar sobre segmentos já disponíveis.
 */
import { ref, watch, onUnmounted, computed } from 'vue'
import { Volume2, Square, Pause, Play, SkipBack, SkipForward } from 'lucide-vue-next'
import {
  segmentosDaCena,
  criarNarrador,
  suporteTts,
  quandoVozesProntas,
  assinaturaBlocos
} from '@/jornada/tts'

const props = defineProps({
  blocos: { type: Array, default: () => [] },
  /** Só mostrada DEPOIS de a leitura terminar por completo */
  reflexao: { type: String, default: '' }
})

const disponivel = suporteTts()
const aOuvir = ref(false)
const pausado = ref(false)
const indiceActivo = ref(-1)
const velocidade = ref(1)
const erro = ref('')
const mostrouReflexao = ref(false)
const assinaturaActual = ref('')

let narrador = null

const segmentos = computed(() => segmentosDaCena(props.blocos))
const total = computed(() => segmentos.value.length)
const activoKey = computed(() => {
  const i = indiceActivo.value
  if (i < 0 || i >= segmentos.value.length) return null
  return segmentos.value[i].key
})
const progresso = computed(() => {
  if (total.value === 0) return ''
  if (indiceActivo.value < 0) return mostrouReflexao.value ? `${total.value}/${total.value}` : `0/${total.value}`
  return `${indiceActivo.value + 1}/${total.value}`
})

function destruirNarrador() {
  narrador?.parar()
  narrador = null
}

function criar() {
  destruirNarrador()
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
      // Reflexão só no fim — nunca a meio
      if (props.reflexao) mostrouReflexao.value = true
    },
    onErro() {
      aOuvir.value = false
      pausado.value = false
      erro.value = 'Não foi possível iniciar a leitura. Tenta de novo.'
      narrador = null
    }
  })
}

// Só reinicia se o TEXTO da história mudar — não em re-renders do pai
watch(
  () => assinaturaBlocos(props.blocos),
  (nova, antiga) => {
    if (nova === antiga) return
    assinaturaActual.value = nova
    destruirNarrador()
    aOuvir.value = false
    pausado.value = false
    indiceActivo.value = -1
    mostrouReflexao.value = false
    erro.value = ''
  }
)

onUnmounted(() => destruirNarrador())

async function iniciar(desde = 0) {
  erro.value = ''
  mostrouReflexao.value = false
  if (!disponivel) {
    erro.value = 'O teu browser não suporta leitura em voz alta.'
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
  destruirNarrador()
  aOuvir.value = false
  pausado.value = false
  indiceActivo.value = -1
  // Não esconde reflexão se já tinha terminado naturalmente
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
  if (!segmentos.value.length) return
  mostrouReflexao.value = false
  if (!narrador || !aOuvir.value) {
    const i = indiceActivo.value > 0 ? indiceActivo.value - 1 : 0
    iniciar(i)
    return
  }
  pausado.value = false
  aOuvir.value = true
  narrador.anterior()
}

function seguinte() {
  if (!segmentos.value.length) return
  mostrouReflexao.value = false
  if (!narrador || !aOuvir.value) {
    const i =
      indiceActivo.value >= 0
        ? Math.min(indiceActivo.value + 1, segmentos.value.length - 1)
        : 0
    iniciar(i)
    return
  }
  pausado.value = false
  aOuvir.value = true
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

defineExpose({ activoKey, segmentos })
</script>

<template>
  <div class="narrativa-audio">
    <div class="controlos">
      <button
        v-if="!aOuvir"
        type="button"
        class="btn-ouvir"
        :disabled="!disponivel || !segmentos.length"
        @click="iniciar(0)"
      >
        <Volume2 :size="16" />
        Ouvir experiência do João
      </button>

      <template v-else>
        <button type="button" class="btn-ouvir activo" @click="alternarPausa">
          <Pause v-if="!pausado" :size="16" />
          <Play v-else :size="16" />
          {{ pausado ? 'Continuar' : 'Pausar' }}
        </button>
        <button type="button" class="btn-icon" title="Parar" @click="parar">
          <Square :size="14" />
        </button>
      </template>

      <div class="nav-frase">
        <button
          type="button"
          class="btn-icon"
          title="Frase anterior"
          :disabled="!segmentos.length"
          @click="anterior"
        >
          <SkipBack :size="16" />
        </button>
        <span class="progresso" aria-live="polite">{{ progresso }}</span>
        <button
          type="button"
          class="btn-icon"
          title="Frase seguinte"
          :disabled="!segmentos.length"
          @click="seguinte"
        >
          <SkipForward :size="16" />
        </button>
      </div>

      <div class="velocidades" role="group" aria-label="Velocidade da voz">
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

    <p v-if="erro" class="erro-tts">{{ erro }}</p>
    <p v-else-if="!disponivel" class="aviso-tts">
      Leitura em voz alta indisponível neste browser.
    </p>

    <div class="texto-narrado mi-stagger" aria-live="polite">
      <template v-for="(b, bi) in blocos" :key="'b-' + bi">
        <p v-if="b.tipo === 'narrativa'" class="bloco-narrativa">
          <span
            v-for="seg in segmentos.filter((s) => s.key.startsWith(bi + '-') && s.tipo === 'narrativa')"
            :key="seg.key"
            class="frase"
            :class="{ activa: activoKey === seg.key, lida: activoKey && segmentos.findIndex(x => x.key === seg.key) < indiceActivo }"
            role="button"
            tabindex="0"
            :title="'Ler a partir daqui'"
            @click="iniciar(segmentos.findIndex((x) => x.key === seg.key))"
            @keydown.enter="iniciar(segmentos.findIndex((x) => x.key === seg.key))"
          >{{ seg.texto }}&#32;</span>
        </p>
        <figure v-else-if="b.tipo === 'dialogo'" class="bloco-dialogo">
          <figcaption>{{ b.quem }}</figcaption>
          <blockquote>
            <span
              v-for="seg in segmentos.filter((s) => s.key.startsWith(bi + '-') && s.tipo === 'dialogo')"
              :key="seg.key"
              class="frase"
              :class="{ activa: activoKey === seg.key }"
              role="button"
              tabindex="0"
              @click="iniciar(segmentos.findIndex((x) => x.key === seg.key))"
              @keydown.enter="iniciar(segmentos.findIndex((x) => x.key === seg.key))"
            >{{ seg.texto }}&#32;</span>
          </blockquote>
        </figure>
      </template>
    </div>

    <!-- Reflexão: só depois de a leitura terminar por completo -->
    <aside v-if="mostrouReflexao && reflexao" class="reflexao" role="status">
      <p class="reflexao-label">Para pensares</p>
      <p class="reflexao-texto">{{ reflexao }}</p>
    </aside>
  </div>
</template>

<style scoped>
.narrativa-audio { margin-bottom: 0.5rem; }
.controlos {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 1rem;
}
.btn-ouvir {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-family: inherit;
  font-size: 0.84rem;
  font-weight: 600;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  border: 1.5px solid var(--color-secondary-300, #9bb89a);
  background: var(--color-secondary-100, #eef6f2);
  color: var(--color-secondary-700, #2d4a2c);
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(30, 50, 40, 0.04);
}
.btn-ouvir:hover:not(:disabled) { border-color: var(--color-secondary-500); }
.btn-ouvir:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-ouvir.activo {
  background: var(--color-secondary-500);
  border-color: var(--color-secondary-500);
  color: #fff;
}
.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
}
.btn-icon:disabled { opacity: 0.4; cursor: not-allowed; }
.nav-frase {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
.progresso {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  min-width: 2.5rem;
  text-align: center;
}
.velocidades {
  display: inline-flex;
  gap: 0.2rem;
  margin-left: auto;
}
.vel-btn {
  font-family: inherit;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.25rem 0.45rem;
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
.erro-tts, .aviso-tts {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin: 0 0 0.75rem;
}
.erro-tts { color: var(--color-danger, #b33); }
.texto-narrado {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.bloco-narrativa {
  margin: 0;
  font-size: 1.02rem;
  line-height: 1.65;
  color: var(--color-text);
}
.bloco-dialogo {
  margin: 0;
  padding-left: 0.85rem;
  border-left: 3px solid var(--color-secondary-300);
}
.bloco-dialogo figcaption {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-secondary-600);
  margin-bottom: 0.25rem;
}
.bloco-dialogo blockquote {
  margin: 0;
  font-size: 1rem;
  line-height: 1.55;
  color: var(--color-text);
  font-style: normal;
}
.frase {
  transition:
    background var(--motion-base, 220ms) var(--ease-out, ease),
    color var(--motion-fast, 150ms) var(--ease-out, ease),
    box-shadow var(--motion-base, 220ms) var(--ease-out, ease);
  border-radius: 4px;
  cursor: pointer;
  padding: 0.05em 0.12em;
  margin: 0 -0.12em;
}
.frase:hover { background: var(--color-secondary-50, #f4f7ea); }
.frase.activa {
  background: var(--color-secondary-100, #e7edd5);
  color: var(--color-secondary-700, #3f5622);
  box-shadow: inset 0 -2px 0 var(--color-secondary-300, #b6c983);
  box-decoration-break: clone;
  -webkit-box-decoration-break: clone;
}
.btn-icon {
  transition:
    color var(--motion-fast, 150ms) var(--ease-out, ease),
    border-color var(--motion-fast, 150ms) var(--ease-out, ease),
    transform var(--motion-fast, 150ms) var(--ease-out, ease),
    background var(--motion-fast, 150ms) var(--ease-out, ease);
}
.btn-icon:hover:not(:disabled) {
  color: var(--color-secondary-600);
  border-color: var(--color-secondary-300);
  background: var(--color-secondary-50, #f4f7ea);
}
.btn-icon:active:not(:disabled) {
  transform: scale(0.94);
}
.vel-btn {
  transition:
    border-color var(--motion-fast, 150ms) var(--ease-out, ease),
    background var(--motion-fast, 150ms) var(--ease-out, ease),
    color var(--motion-fast, 150ms) var(--ease-out, ease);
}
.reflexao {
  margin-top: 1.25rem;
  padding: 0.9rem 1rem;
  border-radius: 12px;
  border: 1px dashed var(--color-secondary-300);
  background: var(--color-surface-alt, var(--color-bg));
}
.reflexao-label {
  margin: 0 0 0.35rem;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-secondary-600);
}
.reflexao-texto {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--color-text);
}
</style>
