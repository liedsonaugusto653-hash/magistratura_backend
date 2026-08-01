<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useEstatisticaStore } from '@/stores/estatistica'
import {
  BarChart3,
  BookOpen,
  Layers,
  Flame,
  Clock,
  Target,
  ChevronRight,
  Sparkles
} from 'lucide-vue-next'
import { LoadingState, ErrorState } from '@/components/ui'
import { PageHero } from '@/components/ui'

const store = useEstatisticaStore()
const router = useRouter()
onMounted(() => store.carregar({ forcar: true }))

const d = computed(() => store.dados || {})

const cards = computed(() => [
  {
    key: 'questoes',
    icon: BookOpen,
    valor: d.value.questoesRespondidas ?? 0,
    label: 'Questões respondidas',
    extra: `${d.value.percentagemSucessoQuestoes ?? 0}% de acerto`,
    to: '/questoes'
  },
  {
    key: 'flash',
    icon: Layers,
    valor: d.value.flashcardsConcluidos ?? 0,
    label: 'Flashcards revistos',
    extra: `${d.value.percentagemSucessoFlashcards ?? 0}% de acerto`,
    to: '/flashcards'
  },
  {
    key: 'horas',
    icon: Clock,
    valor: `${d.value.horasEstudo ?? 0}h`,
    label: 'Tempo de estudo',
    extra: d.value.ultimaAtividade
      ? `Última actividade registada`
      : 'Ainda sem sessões registadas',
    to: null
  },
  {
    key: 'streak',
    icon: Flame,
    valor: d.value.diasConsecutivos ?? 0,
    label: 'Dias consecutivos',
    extra: (d.value.diasConsecutivos ?? 0) > 0 ? 'Mantém o ritmo' : 'Começa hoje',
    to: null
  }
])

function ir(to) {
  if (to) router.push(to)
}
</script>

<template>
  <div class="page stats-page">
    <PageHero
      eyebrow="Estatísticas"
      title="O teu progresso"
      lead="Uma visão clara do tempo de estudo, questões e consistência."
      art="stats"
    />

    <LoadingState v-if="store.carregando" message="A carregar estatísticas…" />
    <ErrorState v-else-if="store.erro" :message="store.erro" @retry="store.carregar({ forcar: true })" />

    <template v-else-if="store.dados">
      <div class="grid-cards">
        <button
          v-for="c in cards"
          :key="c.key"
          type="button"
          class="stat-card"
          :class="{ clicavel: !!c.to }"
          :disabled="!c.to"
          @click="ir(c.to)"
        >
          <span class="stat-icon"><component :is="c.icon" :size="20" /></span>
          <span class="stat-valor">{{ c.valor }}</span>
          <span class="stat-label">{{ c.label }}</span>
          <span class="stat-extra">{{ c.extra }}</span>
          <ChevronRight v-if="c.to" :size="16" class="stat-seta" />
        </button>
      </div>

      <section class="painel">
        <header class="painel-head">
          <BarChart3 :size="18" />
          <strong>Resumo de estudo</strong>
        </header>
        <div class="barras">
          <div class="barra-item">
            <div class="barra-meta">
              <span>Acerto em questões</span>
              <span>{{ d.percentagemSucessoQuestoes ?? 0 }}%</span>
            </div>
            <div class="barra-track">
              <div
                class="barra-fill"
                :style="{ width: Math.min(100, d.percentagemSucessoQuestoes ?? 0) + '%' }"
              />
            </div>
          </div>
          <div class="barra-item">
            <div class="barra-meta">
              <span>Acerto em flashcards</span>
              <span>{{ d.percentagemSucessoFlashcards ?? 0 }}%</span>
            </div>
            <div class="barra-track">
              <div
                class="barra-fill alt"
                :style="{ width: Math.min(100, d.percentagemSucessoFlashcards ?? 0) + '%' }"
              />
            </div>
          </div>
        </div>
        <p class="painel-nota">
          <Target :size="14" />
          Os valores actualizam-se quando respondes questões ou revês flashcards.
        </p>
      </section>

      <section class="atalhos">
        <button type="button" class="atalho" @click="router.push('/questoes')">
          <BookOpen :size="16" /> Praticar questões
        </button>
        <button type="button" class="atalho" @click="router.push('/flashcards')">
          <Layers :size="16" /> Rever flashcards
        </button>
        <button type="button" class="atalho" @click="router.push('/caminhada')">
          <Sparkles :size="16" /> Continuar Experiências
        </button>
        <button type="button" class="atalho" @click="router.push('/biblioteca')">
          <BookOpen :size="16" /> Abrir Biblioteca
        </button>
      </section>
    </template>

    <p v-else class="vazio">Ainda não há dados de estudo para mostrar.</p>
  </div>
</template>

<style scoped>
.stats-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 1.5rem 1.15rem 3.5rem;
}
.hero { margin-bottom: 1.5rem; }
.eyebrow {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-secondary-600);
  margin-bottom: 0.35rem;
}
.hero h1 {
  margin: 0 0 0.4rem;
  font-size: clamp(1.45rem, 3vw, 1.7rem);
  font-weight: 700;
  letter-spacing: -0.02em;
}
.lead {
  margin: 0;
  font-size: 0.92rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  max-width: 36rem;
}
.grid-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}
@media (min-width: 720px) {
  .grid-cards { grid-template-columns: repeat(4, 1fr); }
}
.stat-card {
  position: relative;
  text-align: left;
  font-family: inherit;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: 16px;
  padding: 1rem 0.95rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  box-shadow: 0 1px 4px rgba(30, 50, 40, 0.04);
}
.stat-card.clicavel {
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.stat-card.clicavel:hover {
  border-color: var(--color-secondary-300);
  box-shadow: 0 2px 10px rgba(30, 50, 40, 0.07);
}
.stat-card:disabled { cursor: default; }
.stat-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--color-secondary-100, #e8f2eb);
  color: var(--color-secondary-700);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.35rem;
}
.stat-valor {
  font-size: 1.45rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
}
.stat-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text);
}
.stat-extra {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.3;
}
.stat-seta {
  position: absolute;
  top: 0.85rem;
  right: 0.75rem;
  color: var(--color-text-muted);
  opacity: 0.5;
}
.painel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 1.15rem 1.2rem;
  margin-bottom: 1.25rem;
}
.painel-head {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 1rem;
  color: var(--color-secondary-700);
  font-size: 0.92rem;
}
.barra-item { margin-bottom: 0.85rem; }
.barra-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: var(--color-text);
}
.barra-track {
  height: 8px;
  border-radius: 99px;
  background: rgba(61, 107, 79, 0.12);
  overflow: hidden;
}
.barra-fill {
  height: 100%;
  border-radius: 99px;
  background: var(--color-secondary-500, #4a7c59);
  transition: width 0.5s ease;
}
.barra-fill.alt {
  background: var(--color-primary-500, #c4785a);
}
.painel-nota {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0.5rem 0 0;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.atalhos {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.atalho {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-family: inherit;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-secondary-700);
  cursor: pointer;
}
.atalho:hover {
  border-color: var(--color-secondary-400);
  background: var(--color-secondary-50, #f4faf6);
}
.vazio {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
