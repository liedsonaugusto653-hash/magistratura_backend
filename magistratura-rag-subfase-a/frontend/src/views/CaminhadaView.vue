<script setup>
/**
 * Currículo narrativo — Missão → competências → experiências.
 */
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useJornadaStore } from '@/stores/jornada'
import { LoadingState } from '@/components/ui'
import { ChevronRight, BookOpen, Sparkles } from 'lucide-vue-next'
import PersonagemAvatar from '@/components/brand/PersonagemAvatar.vue'
import { PageHero } from '@/components/ui'
import { progressoCompetencias } from '@/jornada/seed'

const jornada = useJornadaStore()
const router = useRouter()

onMounted(() => jornada.carregar())

const visiveis = computed(() => jornada.momentosVisiveis)

const total = computed(() => visiveis.value.length)
const vividas = computed(
  () => visiveis.value.filter((m) => jornada.isMomentoConcluido(m.id)).length
)
const progressoPct = computed(() =>
  total.value ? Math.round((vividas.value / total.value) * 100) : 0
)

const missao = computed(() => jornada.seed?.missao || null)

const competenciasProgresso = computed(() =>
  progressoCompetencias(jornada.progress?.concluidos || [])
)

const porFase = computed(() => {
  const grupos = [
    { chave: 'observar', titulo: 'Observar', subtitulo: 'Compreender sem decidir', items: [] },
    { chave: 'compreender', titulo: 'Compreender', subtitulo: 'Normas, fontes e órgãos', items: [] },
    { chave: 'interpretar', titulo: 'Interpretar', subtitulo: 'Perguntas antes das conclusões', items: [] },
    { chave: 'decidir', titulo: 'Decidir', subtitulo: 'Facto → norma → consequência', items: [] },
    { chave: 'iniciante', titulo: 'Começos', subtitulo: 'Primeiros passos', items: [] },
    { chave: 'estudante', titulo: 'Percursos', subtitulo: 'A meio do caminho', items: [] },
    { chave: 'candidato', titulo: 'Decisões', subtitulo: 'Quando o peso aumenta', items: [] },
    { chave: 'outra', titulo: 'Outras', subtitulo: 'Experiências', items: [] }
  ]
  for (const m of visiveis.value) {
    const chave = m.fasePedagogica || m.fase || 'outra'
    const g = grupos.find((x) => x.chave === chave) || grupos.find((x) => x.chave === 'outra')
    g.items.push(m)
  }
  return grupos.filter((g) => g.items.length)
})

function abrirExperiencia(m) {
  jornada.irParaMomento(m.id)
  router.push({ name: 'experiencia-joao', params: { momentoId: m.id } })
}

function continuar() {
  const actual = jornada.momentoActual
  if (actual && !jornada.isMomentoConcluido(actual.id)) {
    abrirExperiencia(actual)
    return
  }
  const proxima = visiveis.value.find((m) => !jornada.isMomentoConcluido(m.id))
  if (proxima) abrirExperiencia(proxima)
  else if (visiveis.value[0]) abrirExperiencia(visiveis.value[0])
}
</script>

<template>
  <div class="page cam-page">
    <PageHero
      eyebrow="Currículo narrativo"
      :title="missao?.titulo || 'Experiências'"
      :lead="missao?.descricao || 'Acompanha o crescimento do protagonista enquanto constróis competências jurídicas.'"
      art="caminhada"
    />

    <section v-if="!jornada.carregando && competenciasProgresso.length" class="mapa-competencias card-comp">
      <header class="comp-head">
        <h2>Competências da missão</h2>
        <p>Cada experiência serve uma capacidade concreta — não apenas um tema.</p>
      </header>
      <ul class="comp-lista">
        <li
          v-for="c in competenciasProgresso"
          :key="c.id"
          class="comp-item"
          :class="{ completa: c.completa }"
        >
          <span class="comp-pct">{{ c.percentagem }}%</span>
          <div class="comp-body">
            <strong>{{ c.titulo }}</strong>
            <span class="comp-meta">{{ c.feitas }}/{{ c.total }} experiências · {{ c.fasePedagogica }}</span>
          </div>
        </li>
      </ul>
    </section>

    <LoadingState v-if="jornada.carregando" message="A carregar os capítulos…" />

    <template v-else>
      <section
        v-for="grupo in porFase"
        :key="grupo.chave"
        class="fase"
      >
        <header class="fase-head">
          <span class="fase-badge">{{ grupo.titulo }}</span>
          <h2>{{ grupo.subtitulo }}</h2>
        </header>

        <ul class="lista">
          <li v-for="m in grupo.items" :key="m.id">
            <button
              type="button"
              class="card"
              :class="{
                activo: jornada.momentoActual?.id === m.id && !jornada.isMomentoConcluido(m.id),
                feito: jornada.isMomentoConcluido(m.id)
              }"
              @click="abrirExperiencia(m)"
            >
              <span class="ordem" aria-hidden="true">{{ String(m.ordem).padStart(2, '0') }}</span>
              <span class="corpo">
                <span class="titulo">{{ m.titulo }}</span>
                <span v-if="m.conceito?.rotulo" class="conceito">
                  <BookOpen :size="13" />
                  {{ m.conceito.rotulo }}
                </span>
              </span>
              <span class="lado">
                <span v-if="jornada.isMomentoConcluido(m.id)" class="tag viva">Vivida</span>
                <span
                  v-else-if="jornada.momentoActual?.id === m.id"
                  class="tag agora"
                >
                  <Sparkles :size="12" />
                  Agora
                </span>
                <ChevronRight :size="18" class="seta" />
              </span>
            </button>
          </li>
        </ul>
      </section>

      <p v-if="!visiveis.length" class="aviso">
        Ainda não há capítulos disponíveis — as experiências crescem com a legislação
        processada na Biblioteca.
      </p>
    </template>
  </div>
</template>

<style scoped>
.cam-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 1.75rem 1.25rem 4.5rem;
}

/* —— Hero —— */
.hero {
  display: grid;
  gap: 1.25rem;
  margin-bottom: 2.25rem;
}
@media (min-width: 640px) {
  .hero {
    grid-template-columns: 1fr minmax(200px, 240px);
    align-items: stretch;
  }
}
.eyebrow {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-secondary-600, #3d6b4f);
  margin-bottom: 0.4rem;
}
.hero h1 {
  margin: 0 0 0.55rem;
  font-size: clamp(1.6rem, 3vw, 1.9rem);
  font-family: var(--font-heading, inherit);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text, #1a1f1c);
}
.lead {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.55;
  color: var(--color-text-muted, #5c6b62);
  max-width: 36rem;
}

.hero-card {
  background: linear-gradient(
    145deg,
    var(--color-secondary-100, #e8f2eb) 0%,
    var(--color-surface, #fff) 100%
  );
  border: 1px solid var(--color-secondary-200, #c5ddce);
  border-radius: 16px;
  padding: 1.15rem 1.2rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 1rem;
  box-shadow: 0 1px 2px rgba(30, 50, 40, 0.04);
}
.progress-num {
  display: block;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-secondary-800, #243d2e);
  letter-spacing: -0.02em;
}
.progress-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-weight: 500;
}
.progress-bar {
  margin-top: 0.65rem;
  height: 6px;
  border-radius: 99px;
  background: rgba(61, 107, 79, 0.12);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 99px;
  background: var(--color-secondary-500, #4a7c59);
  transition: width 0.4s ease;
}
.btn-continuar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  width: 100%;
  font-family: inherit;
  font-size: 0.86rem;
  font-weight: 600;
  padding: 0.6rem 1rem;
  border-radius: 999px;
  border: none;
  background: var(--color-secondary-600, #3d6b4f);
  color: #fff;
  cursor: pointer;
  transition: filter 0.15s ease, transform 0.1s ease;
}
.btn-continuar:hover {
  filter: brightness(1.06);
}
.btn-continuar:active {
  transform: scale(0.98);
}

/* —— Fases —— */
.fase {
  margin-bottom: 2rem;
}
.fase-head {
  display: flex;
  align-items: baseline;
  gap: 0.65rem;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e4ebe6);
}
.fase-badge {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-secondary-600);
  background: var(--color-secondary-100, #e8f2eb);
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
}
.fase-head h2 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
}

.lista {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.card {
  width: 100%;
  text-align: left;
  border: 1px solid var(--color-border, #e4ebe6);
  background: var(--color-surface, #fff);
  border-radius: 14px;
  padding: 0.9rem 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  font-family: inherit;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}
.card:hover {
  border-color: var(--color-secondary-300, #9bb89a);
  box-shadow: 0 2px 8px rgba(30, 50, 40, 0.06);
}
.card.activo {
  border-color: var(--color-secondary-400, #6a9b78);
  background: var(--color-secondary-50, #f4faf6);
  box-shadow: 0 0 0 1px var(--color-secondary-200, #c5ddce);
}
.card.feito {
  opacity: 0.88;
}
.card.feito .titulo {
  color: var(--color-text-muted);
}

.ordem {
  flex-shrink: 0;
  width: 2.1rem;
  height: 2.1rem;
  border-radius: 10px;
  background: var(--color-secondary-100, #e8f2eb);
  color: var(--color-secondary-700, #2d4a2c);
  font-size: 0.78rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card.activo .ordem {
  background: var(--color-secondary-500, #4a7c59);
  color: #fff;
}
.card.feito .ordem {
  background: var(--color-border, #e4ebe6);
  color: var(--color-text-muted);
}

.corpo {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.titulo {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.3;
}
.conceito {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lado {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
}
.tag {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  padding: 0.2rem 0.45rem;
  border-radius: 999px;
}
.tag.viva {
  color: var(--color-secondary-700);
  background: var(--color-secondary-100, #e8f2eb);
}
.tag.agora {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  color: #fff;
  background: var(--color-secondary-500, #4a7c59);
}
.seta {
  color: var(--color-text-muted);
  opacity: 0.55;
}
.card:hover .seta {
  opacity: 1;
  color: var(--color-secondary-600);
}

.aviso {
  font-size: 0.9rem;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.hero {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}
.hero-art {
  flex-shrink: 0;
  padding: 0.35rem;
  border-radius: 16px;
  background: var(--color-secondary-50, #f4f7ea);
  border: 1px solid var(--color-secondary-100);
}

.card-comp {
  margin: 0 0 1.75rem;
  padding: 1.1rem 1.2rem 1.2rem;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: var(--color-surface, #fff);
}
.comp-head h2 { margin: 0 0 0.25rem; font-size: 1rem; }
.comp-head p { margin: 0 0 0.85rem; font-size: 0.82rem; color: var(--color-text-muted); }
.comp-lista { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.45rem; }
.comp-item {
  display: flex; gap: 0.75rem; align-items: flex-start;
  padding: 0.55rem 0.65rem; border-radius: 10px; border: 1px solid transparent;
  background: var(--color-surface-alt, #f7f9f6);
}
.comp-item.completa {
  border-color: var(--color-secondary-300, #9bb86a);
  background: color-mix(in srgb, var(--color-secondary-50, #f4faf6) 85%, white);
}
.comp-pct {
  font-size: 0.78rem; font-weight: 700; color: var(--color-secondary-700);
  min-width: 2.4rem; font-variant-numeric: tabular-nums;
}
.comp-body { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
.comp-body strong { font-size: 0.88rem; font-weight: 600; }
.comp-meta { font-size: 0.72rem; color: var(--color-text-muted); text-transform: capitalize; }

</style>
