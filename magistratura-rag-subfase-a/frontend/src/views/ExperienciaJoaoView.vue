<script setup>
/**
 * Página de uma experiência desta pessoa.
 */
import { onMounted, computed, ref, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useJornadaStore } from '@/stores/jornada'
import { resolverCta } from '@/jornada/resolver'
import {
  blocosDoMomento,
  ctaDoMomento,
  reflexaoParaMomento
} from '@/jornada/seed'
import NarrativaAudio from '@/components/caminhada/NarrativaAudio.vue'
import { LoadingState } from '@/components/ui'
import { BookOpen, Sparkles, ChevronRight, ChevronLeft, Scale } from 'lucide-vue-next'
import FeatureArt from '@/components/brand/FeatureArt.vue'

const jornada = useJornadaStore()
const route = useRoute()
const router = useRouter()
const aResolverCta = ref(false)

onMounted(async () => {
  await jornada.carregar()
  const id = route.params.momentoId
  if (id) jornada.irParaMomento(String(id))
})

watch(
  () => route.params.momentoId,
  (id) => {
    if (id) jornada.irParaMomento(String(id))
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      window.speechSynthesis.cancel()
    }
  }
)

onUnmounted(() => {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
})

const momento = computed(() => {
  const id = route.params.momentoId
  if (id) {
    return jornada.seed.momentos.find((m) => m.id === id) || jornada.momentoActual
  }
  return jornada.momentoActual
})

const blocos = computed(() => blocosDoMomento(momento.value, 0))
const cta = computed(() => ctaDoMomento(momento.value))
const reflexao = computed(() => reflexaoParaMomento(momento.value))

const faseRotulo = computed(() => {
  const m = momento.value
  if (!m) return null
  const mapa = {
    observar: 'Fase · Observar',
    compreender: 'Fase · Compreender',
    interpretar: 'Fase · Interpretar',
    decidir: 'Fase · Decidir'
  }
  if (m.fasePedagogica && mapa[m.fasePedagogica]) return mapa[m.fasePedagogica]
  const f = m.fase
  if (f === 'iniciante') return 'Fase · Iniciante'
  if (f === 'estudante') return 'Fase · Estudante'
  if (f === 'candidato') return 'Fase · Candidato'
  return null
})

const competenciaRotulo = computed(() => momento.value?.competencia?.titulo || null)
const estadoCognitivoRotulo = computed(
  () => momento.value?.estadoCognitivoMeta?.rotulo || null
)
const saidaEsperada = computed(() => momento.value?.saidaEsperada || null)
const ganchoProxima = computed(() => momento.value?.ganchoProxima || null)
const perguntaCentral = computed(() => momento.value?.perguntaCentral || null)

const visiveis = computed(() => jornada.momentosVisiveis)
const indiceNaLista = computed(() =>
  visiveis.value.findIndex((m) => m.id === momento.value?.id)
)
const anterior = computed(() =>
  indiceNaLista.value > 0 ? visiveis.value[indiceNaLista.value - 1] : null
)
const seguinte = computed(() =>
  indiceNaLista.value >= 0 && indiceNaLista.value < visiveis.value.length - 1
    ? visiveis.value[indiceNaLista.value + 1]
    : null
)

async function onCta(c) {
  if (!c || aResolverCta.value) return
  aResolverCta.value = true
  try {
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      window.speechSynthesis.cancel()
    }
    const dest = await resolverCta(c)
    if (dest.query) await router.push({ path: dest.path, query: dest.query })
    else await router.push(dest.path)
  } finally {
    aResolverCta.value = false
  }
}

function irCapitulos() {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
  router.push({ name: 'caminhada' })
}

async function concluirESeguir() {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
  await jornada.avancarCena()
  if (seguinte.value) {
    router.push({ name: 'experiencia-joao', params: { momentoId: seguinte.value.id } })
  } else {
    router.push({ name: 'caminhada' })
  }
}

function irExperiencia(m) {
  if (!m) return
  router.push({ name: 'experiencia-joao', params: { momentoId: m.id } })
}
</script>

<template>
  <div class="page exp-page">
    <nav class="top-nav">
      <button type="button" class="link-voltar" @click="irCapitulos">
        <ChevronLeft :size="16" />
        Capítulos
      </button>
      <span v-if="momento" class="top-progress">
        {{ indiceNaLista >= 0 ? indiceNaLista + 1 : '—' }}
        <span class="sep">/</span>
        {{ visiveis.length }}
      </span>
    </nav>

    <LoadingState v-if="jornada.carregando" message="A abrir a experiência…" />

    <article v-else-if="momento" class="experiencia">
      <header class="exp-header">
        <div class="exp-art" aria-hidden="true">
          <FeatureArt variant="caminhada" :size="40" />
        </div>
        <div class="exp-meta">
          <span v-if="faseRotulo" class="fase-pill">{{ faseRotulo }}</span>
          <span class="exp-num">Experiência {{ String(momento.ordem).padStart(2, '0') }}</span>
        </div>
        <h1 class="exp-titulo">{{ momento.titulo }}</h1>
        <p v-if="perguntaCentral" class="pergunta-central">{{ perguntaCentral }}</p>
        <div class="chips-pedagogicos">
          <div v-if="momento.conceito?.rotulo" class="conceito-chip">
            <Scale :size="14" />
            <span>{{ momento.conceito.rotulo }}</span>
          </div>
          <div v-if="competenciaRotulo" class="conceito-chip chip-comp">
            <span>{{ competenciaRotulo }}</span>
          </div>
          <div v-if="estadoCognitivoRotulo" class="conceito-chip chip-estado">
            <span>{{ estadoCognitivoRotulo }}</span>
          </div>
        </div>
      </header>

      <section class="seccao seccao-exp">
        <h2 class="seccao-titulo">Experiência</h2>
        <NarrativaAudio :blocos="blocos" :reflexao="reflexao" />
      </section>

      <section v-if="saidaEsperada || ganchoProxima" class="seccao seccao-pedagogica">
        <div v-if="saidaEsperada" class="box-saida">
          <h3>O que deves ser capaz de fazer</h3>
          <p>{{ saidaEsperada }}</p>
        </div>
        <div v-if="ganchoProxima" class="box-gancho">
          <h3>A seguir</h3>
          <p>{{ ganchoProxima }}</p>
        </div>
      </section>

      <footer class="exp-footer">
        <button
          v-if="cta"
          type="button"
          class="btn-cta"
          :disabled="aResolverCta"
          @click="onCta(cta)"
        >
          <BookOpen
            v-if="cta.tipo?.includes('biblioteca') || cta.tipo === 'abrir_artigo'"
            :size="16"
          />
          <Sparkles v-else-if="cta.tipo === 'abrir_tutor'" :size="16" />
          {{ aResolverCta ? 'A abrir…' : cta.label }}
        </button>

        <div class="nav-exp">
          <button
            v-if="anterior"
            type="button"
            class="btn-sec"
            @click="irExperiencia(anterior)"
          >
            <ChevronLeft :size="16" />
            Anterior
          </button>
          <span v-else class="nav-spacer" />
          <button type="button" class="btn-seguir" @click="concluirESeguir">
            {{ seguinte ? 'Próxima experiência' : 'Voltar aos capítulos' }}
            <ChevronRight :size="16" />
          </button>
        </div>
      </footer>
    </article>

    <p v-else class="aviso">
      Experiência não encontrada.
      <button type="button" class="link-inline" @click="irCapitulos">Ver capítulos</button>
    </p>
  </div>
</template>

<style scoped>
.exp-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 1.15rem 1.15rem 4.5rem;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.15rem;
}
.link-voltar {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-family: inherit;
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-secondary-700, #2d4a2c);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.3rem 0;
  border-radius: 6px;
}
.link-voltar:hover {
  color: var(--color-secondary-500, #4a7c59);
}
.top-progress {
  font-size: 0.8rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-muted);
}
.top-progress .sep {
  opacity: 0.45;
  margin: 0 0.15rem;
}

.experiencia {
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e4ebe6);
  border-radius: 20px;
  padding: 1.6rem 1.4rem 1.4rem;
  box-shadow: 0 2px 12px rgba(30, 50, 40, 0.04);
}

.exp-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-border, #e4ebe6);
}
.exp-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.55rem;
}
.fase-pill {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-secondary-700);
  background: var(--color-secondary-100, #e8f2eb);
  padding: 0.22rem 0.55rem;
  border-radius: 999px;
}
.exp-num {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
}
.exp-titulo {
  margin: 0 0 0.75rem;
  font-size: clamp(1.3rem, 3vw, 1.5rem);
  font-family: var(--font-heading, inherit);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.25;
  color: var(--color-text, #1a1f1c);
}
.conceito-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
  color: var(--color-secondary-800, #243d2e);
  background: var(--color-secondary-50, #f4faf6);
  border: 1px solid var(--color-secondary-200, #c5ddce);
  padding: 0.4rem 0.7rem;
  border-radius: 10px;
  max-width: 100%;
  line-height: 1.35;
}

.seccao {
  margin-bottom: 1.35rem;
}
.seccao-titulo {
  margin: 0 0 0.55rem;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-secondary-600, #3d6b4f);
}
.corpo-texto {
  margin: 0;
  font-size: 0.98rem;
  line-height: 1.6;
  color: var(--color-text);
}

.card-soft {
  padding: 1rem 1.05rem;
  border-radius: 14px;
  background: var(--color-secondary-50, #f4faf6);
  border: 1px solid var(--color-secondary-100, #e8f2eb);
}
.card-dash {
  padding: 1rem 1.05rem;
  border-radius: 14px;
  border: 1.5px dashed var(--color-secondary-300, #9bb89a);
  background: var(--color-surface-alt, #fafcfb);
}
.pergunta {
  margin: 0;
  font-size: 1.02rem;
  font-weight: 500;
  line-height: 1.5;
  color: var(--color-text);
}
.base-lista {
  margin: 0;
  padding-left: 1.15rem;
  font-size: 0.9rem;
  color: var(--color-text-muted);
  line-height: 1.5;
}
.base-lista li {
  margin-bottom: 0.25rem;
}
.card-desafio {
  padding: 1rem 1.05rem;
  border-radius: 14px;
  background: linear-gradient(
    135deg,
    var(--color-primary-50, #f6f8f2) 0%,
    var(--color-secondary-50, #f4faf6) 100%
  );
  border: 1px solid var(--color-border, #e4ebe6);
}

.exp-footer {
  margin-top: 1.75rem;
  padding-top: 1.2rem;
  border-top: 1px solid var(--color-border, #e4ebe6);
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.nav-exp {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  align-items: center;
  justify-content: space-between;
}
.nav-spacer {
  flex: 1;
}
.btn-cta,
.btn-seguir,
.btn-sec {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-family: inherit;
  font-size: 0.88rem;
  font-weight: 600;
  padding: 0.6rem 1.1rem;
  border-radius: 999px;
  cursor: pointer;
  border: 1.5px solid var(--color-border, #e4ebe6);
  background: var(--color-surface, #fff);
  color: var(--color-secondary-700, #2d4a2c);
  transition: filter 0.15s ease, border-color 0.15s ease;
}
.btn-cta:hover:not(:disabled) {
  border-color: var(--color-secondary-400);
}
.btn-seguir {
  background: var(--color-secondary-600, #3d6b4f);
  border-color: var(--color-secondary-600, #3d6b4f);
  color: #fff;
}
.btn-seguir:hover {
  filter: brightness(1.06);
}
.btn-cta:disabled {
  opacity: 0.7;
  cursor: wait;
}
.aviso {
  font-size: 0.9rem;
  color: var(--color-text-muted);
}
.link-inline {
  font-family: inherit;
  font-size: inherit;
  color: var(--color-secondary-700);
  background: none;
  border: none;
  text-decoration: underline;
  cursor: pointer;
}

.exp-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem 1rem;
  align-items: start;
}
.exp-art {
  grid-row: 1 / span 3;
  padding: 0.35rem;
  border-radius: 14px;
  background: var(--color-secondary-50, #f4f7ea);
  border: 1px solid var(--color-border);
}
.exp-meta { grid-column: 2; }
.exp-titulo { grid-column: 2; }
.conceito-chip { grid-column: 2; }
@media (max-width: 480px) {
  .exp-header { grid-template-columns: 1fr; }
  .exp-art { grid-row: auto; width: fit-content; }
  .exp-meta, .exp-titulo, .conceito-chip { grid-column: 1; }
}

.pergunta-central {
  margin: 0.35rem 0 0.75rem;
  font-size: 1.02rem;
  font-weight: 600;
  color: var(--color-secondary-700, #2d4a2c);
  line-height: 1.35;
}
.chips-pedagogicos {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: 0.35rem;
}
.chip-comp {
  background: color-mix(in srgb, var(--color-secondary-100, #e8f2eb) 80%, white);
}
.chip-estado {
  border-style: dashed;
}
.seccao-pedagogica {
  display: grid;
  gap: 0.75rem;
  margin: 1rem 0 0.25rem;
}
.box-saida,
.box-gancho {
  padding: 0.85rem 1rem;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: var(--color-surface, #fff);
}
.box-saida h3,
.box-gancho h3 {
  margin: 0 0 0.35rem;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}
.box-saida p,
.box-gancho p {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.45;
  color: var(--color-text);
}
.box-gancho {
  border-color: var(--color-secondary-300, #9bb86a);
  background: color-mix(in srgb, var(--color-secondary-50, #f4faf6) 90%, white);
}

</style>
