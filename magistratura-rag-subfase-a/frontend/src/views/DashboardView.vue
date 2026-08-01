<script setup>
import PageHint from '@/components/ui/PageHint.vue'
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useJornadaStore } from '@/stores/jornada'
import { Clock, BookOpen, Layers, TrendingUp } from 'lucide-vue-next'
import { LoadingState, ErrorState, PageHero } from '@/components/ui'
import FeatureArt from '@/components/brand/FeatureArt.vue'

const dashboard = useDashboardStore()
const jornada = useJornadaStore()
const router = useRouter()

onMounted(async () => {
  await Promise.all([dashboard.carregar(), jornada.carregar().catch(() => {})])
})

const gancho = computed(() => jornada.ganchoActual)
const r = computed(() => dashboard.resumo)
const percAcerto = computed(() => {
  const v = r.value.percentagemSucessoQuestoes
  return v != null ? Math.round(Number(v) * 10) / 10 : 0
})

const destaques = [
  { label: 'Experiências', to: '/caminhada', art: 'caminhada', desc: 'Histórias que formam o olhar' },
  { label: 'Tutor IA', to: '/tutor', art: 'tutor', desc: 'Pergunta com base na lei' },
  { label: 'Biblioteca', to: '/biblioteca', art: 'biblioteca', desc: 'Diplomas e artigos' }
]
</script>

<template>
  <div class="page dash-page">
    <PageHero
      eyebrow="Painel"
      title="Bem-vindo de volta"
      art="brand"
      :art-size="56"
    >
      <template #lead>
        <span v-if="gancho">{{ gancho }}</span>
        <span v-else>Continua o estudo pela Biblioteca, pelo Tutor ou pelas Experiências.</span>
      </template>
    </PageHero>

    <PageHint
      text="Os números abaixo são os mesmos da página Estatísticas — uma única fonte de progresso."
    />

    <div class="destaques">
      <button
        v-for="d in destaques"
        :key="d.to"
        type="button"
        class="destaque"
        @click="router.push(d.to)"
      >
        <FeatureArt :variant="d.art" :size="44" />
        <span class="destaque-text">
          <strong>{{ d.label }}</strong>
          <small>{{ d.desc }}</small>
        </span>
      </button>
    </div>

    <LoadingState v-if="dashboard.carregando" message="A carregar o painel…" />
    <ErrorState
      v-else-if="dashboard.erro"
      :message="dashboard.erro"
      @retry="dashboard.carregar()"
    />

    <template v-else>
      <div class="grid-stats">
        <div class="stat">
          <Clock :size="18" />
          <strong>{{ r.horasEstudo ?? 0 }}h</strong>
          <span>Horas de estudo</span>
        </div>
        <div class="stat">
          <BookOpen :size="18" />
          <strong>{{ r.questoesRespondidas ?? 0 }}</strong>
          <span>Questões</span>
        </div>
        <div class="stat">
          <Layers :size="18" />
          <strong>{{ r.flashcardsConcluidos ?? 0 }}</strong>
          <span>Flashcards</span>
        </div>
        <div class="stat">
          <TrendingUp :size="18" />
          <strong>{{ percAcerto }}%</strong>
          <span>Acerto questões</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dash-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 1.5rem 1.15rem 3rem;
}
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.25rem;
  padding: 1.15rem 1.25rem;
  border-radius: var(--radius-lg, 22px);
  background:
    radial-gradient(circle at 90% 20%, var(--color-primary-100), transparent 50%),
    radial-gradient(circle at 10% 80%, var(--color-secondary-100), transparent 45%),
    var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}
.hero-copy { flex: 1; min-width: 0; }
.hero-art {
  flex-shrink: 0;
  animation: hero-in 0.5s var(--ease-out, ease) both;
}
@keyframes hero-in {
  from { opacity: 0; transform: scale(0.92) translateY(6px); }
  to { opacity: 1; transform: none; }
}
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
  margin: 0 0 0.45rem;
  font-size: clamp(1.45rem, 3vw, 1.75rem);
  font-weight: 700;
  letter-spacing: -0.02em;
}
.lead, .hook {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--color-text-muted);
  max-width: 36rem;
}
.hook { color: var(--color-text); font-size: 1.02rem; }

.destaques {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.65rem;
  margin-bottom: 1.5rem;
}
@media (min-width: 640px) {
  .destaques { grid-template-columns: repeat(3, 1fr); }
}
.destaque {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-align: left;
  padding: 0.85rem 1rem;
  border-radius: 16px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color var(--motion-fast, 150ms) ease,
    box-shadow var(--motion-base, 220ms) ease,
    transform var(--motion-base, 220ms) ease;
}
.destaque:hover {
  border-color: var(--color-secondary-300);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}
.destaque-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.destaque-text strong {
  font-size: 0.9rem;
  color: var(--color-secondary-700);
}
.destaque-text small {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.grid-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.65rem;
}
@media (min-width: 640px) {
  .grid-stats { grid-template-columns: repeat(4, 1fr); }
}
.stat {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 0.95rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: var(--color-secondary-700);
}
.stat strong {
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.02em;
}
.stat span {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
.stat svg { opacity: 0.85; }

@media (max-width: 520px) {
  .hero { flex-direction: column-reverse; text-align: center; }
  .lead, .hook { max-width: none; }
  .hero-art { margin: 0 auto; }
}
</style>
