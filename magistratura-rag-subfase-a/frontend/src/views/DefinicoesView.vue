<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { lerPrefsUi, GUIA_NIVEIS, normalizarPrefs } from '@/utils/prefsUi'
import { Check, AlertTriangle, PanelLeft, ShieldCheck, Lightbulb, Palette, Compass } from 'lucide-vue-next'
import { PageHero } from '@/components/ui'
import FeatureArt from '@/components/brand/FeatureArt.vue'

const ui = useUiStore()
const auth = useAuthStore()

const prefs = ref(lerPrefsUi())
const mensagem = ref('')
const erro = ref('')
const aGuardar = ref(false)

const temas = [
  {
    value: 'claro',
    label: 'Claro',
    desc: 'Bege e verde — leitura diurna',
    preview: 'claro'
  },
  {
    value: 'escuro',
    label: 'Escuro',
    desc: 'Contraste suave para a noite',
    preview: 'escuro'
  },
  {
    value: 'sistema',
    label: 'Sistema',
    desc: 'Segue o tema do teu dispositivo',
    preview: 'sistema'
  }
]

const toggles = computed(() => [
  {
    key: 'sidebarIniciaColapsada',
    icon: PanelLeft,
    title: 'Menu lateral compacto',
    desc: 'Ao guardar, o menu fica só com ícones (ou expande se desligares).'
  },
  {
    key: 'confirmarAntesDeEliminar',
    icon: ShieldCheck,
    title: 'Confirmar eliminações',
    desc: 'Pede confirmação antes de apagar diplomas, questões, etc.'
  },
  {
    key: 'mostrarDicas',
    icon: Lightbulb,
    title: 'Mostrar dicas',
    desc: 'Textos de ajuda nas páginas principais.'
  }
])

onMounted(async () => {
  try {
    if (auth.token && !auth.utilizador) {
      await auth.carregarUtilizadorAtual()
    }
  } catch {
    /* offline */
  }
  const doServidor = auth.preferencias?.() || {}
  prefs.value = normalizarPrefs({ ...lerPrefsUi(), ...doServidor })
  aplicarTemaLocal(prefs.value.tema)
})

function aplicarTemaLocal(tema) {
  let dark = tema === 'escuro'
  if (tema === 'sistema' && typeof window !== 'undefined') {
    dark = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
}

watch(
  () => prefs.value.tema,
  (tema) => {
    if (tema) aplicarTemaLocal(tema)
  }
)

async function guardar() {
  aGuardar.value = true
  mensagem.value = ''
  erro.value = ''
  const payload = normalizarPrefs(prefs.value)

  try {
    ui.aplicarPrefs?.(payload) || ui.hidratarPrefsDoServidor?.(payload)
    // fallback local
    try {
      const { gravarPrefsUi } = await import('@/utils/prefsUi')
      gravarPrefsUi(payload)
      if (ui.prefs) Object.assign(ui.prefs, payload)
    } catch {
      /* ignore */
    }

    if (!auth.token) {
      mensagem.value = 'Preferências guardadas neste dispositivo (sem sessão).'
      return
    }
    await auth.guardarPreferencias(payload)
    ui.hidratarPrefsDoServidor?.(auth.preferencias())
    prefs.value = { ...ui.prefs }
    mensagem.value = 'Preferências guardadas na tua conta.'
    setTimeout(() => {
      if (mensagem.value.startsWith('Preferências guardadas')) mensagem.value = ''
    }, 3200)
  } catch (e) {
    const d = e?.response?.data
    erro.value =
      d?.mensagem ||
      d?.message ||
      'Não foi possível gravar no servidor. As opções ficaram só neste dispositivo.'
  } finally {
    aGuardar.value = false
  }
}
</script>

<template>
  <div class="page def-page">
    <PageHero
      eyebrow="Conta"
      title="Definições"
      lead="Preferências da plataforma e da tua conta — aplicadas em todo o sistema."
      art="settings"
    />

    <form class="def-form" @submit.prevent="guardar">
      <!-- Comportamento -->
      <section class="def-card">
        <header class="def-card__head">
          <span class="def-card__icon" aria-hidden="true">
            <FeatureArt variant="settings" :size="28" :animated="false" />
          </span>
          <div>
            <h2>Comportamento</h2>
            <p>Como a plataforma reage no dia a dia.</p>
          </div>
        </header>

        <ul class="toggle-list">
          <li v-for="t in toggles" :key="t.key" class="toggle-row">
            <span class="toggle-icon" aria-hidden="true">
              <component :is="t.icon" :size="18" />
            </span>
            <div class="toggle-copy">
              <strong>{{ t.title }}</strong>
              <small>{{ t.desc }}</small>
            </div>
            <label class="switch">
              <input v-model="prefs[t.key]" type="checkbox" />
              <span class="switch-track" />
            </label>
          </li>
        </ul>
      </section>

      <!-- Aparência -->
      <section class="def-card">
        <header class="def-card__head">
          <span class="def-card__icon" aria-hidden="true">
            <Palette :size="20" />
          </span>
          <div>
            <h2>Aparência</h2>
            <p>Tema visual da interface.</p>
          </div>
        </header>

        <div class="tema-grid" role="radiogroup" aria-label="Tema">
          <label
            v-for="tm in temas"
            :key="tm.value"
            class="tema-card"
            :class="{ 'is-active': prefs.tema === tm.value }"
          >
            <input v-model="prefs.tema" type="radio" :value="tm.value" class="sr-only" />
            <span class="tema-preview" :data-preview="tm.preview" aria-hidden="true">
              <span class="tp-bar" />
              <span class="tp-lines">
                <i /><i /><i />
              </span>
            </span>
            <span class="tema-meta">
              <strong>{{ tm.label }}</strong>
              <small>{{ tm.desc }}</small>
            </span>
          </label>
        </div>
      </section>

      <!-- Guia -->
      <section class="def-card">
        <header class="def-card__head">
          <span class="def-card__icon" aria-hidden="true">
            <Compass :size="20" />
          </span>
          <div>
            <h2>Guia de estudo</h2>
            <p>Controla o quão activo é o balão do guia.</p>
          </div>
        </header>

        <div class="guia-grid" role="radiogroup" aria-label="Nível do guia">
          <label
            v-for="g in GUIA_NIVEIS"
            :key="g.value"
            class="guia-card"
            :class="{ 'is-active': prefs.guiaNivel === g.value }"
          >
            <input v-model="prefs.guiaNivel" type="radio" :value="g.value" class="sr-only" />
            <strong>{{ g.label }}</strong>
            <small>{{ g.desc }}</small>
          </label>
        </div>
      </section>

      <div class="def-footer">
        <p v-if="mensagem" class="ok"><Check :size="16" /> {{ mensagem }}</p>
        <p v-if="erro" class="erro"><AlertTriangle :size="16" /> {{ erro }}</p>
        <button type="submit" class="btn-guardar" :disabled="aGuardar">
          {{ aGuardar ? 'A guardar…' : 'Guardar preferências' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.def-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 1.25rem 1.1rem 3.5rem;
}

.def-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.def-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 18px);
  padding: 1.15rem 1.2rem 1.25rem;
  box-shadow: var(--shadow-sm);
}

.def-card__head {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border);
}

.def-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-secondary-50, #f4f7ea);
  border: 1px solid var(--color-border);
  color: var(--color-secondary-700);
  flex-shrink: 0;
}

.def-card__head h2 {
  margin: 0;
  font-size: 1.02rem;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.def-card__head p {
  margin: 0.15rem 0 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

/* Toggles */
.toggle-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.toggle-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.55rem;
  border-radius: 12px;
  transition: background 0.15s ease;
}

.toggle-row:hover {
  background: var(--color-secondary-50, #f7f9f2);
}

.toggle-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-alt, #f7f4ef);
  color: var(--color-secondary-600);
  flex-shrink: 0;
}

.toggle-copy {
  flex: 1;
  min-width: 0;
}

.toggle-copy strong {
  display: block;
  font-size: 0.9rem;
  font-weight: 650;
  color: var(--color-text);
}

.toggle-copy small {
  display: block;
  margin-top: 0.15rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
  line-height: 1.35;
}

/* Switch */
.switch {
  position: relative;
  width: 44px;
  height: 26px;
  flex-shrink: 0;
  cursor: pointer;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
  position: absolute;
}

.switch-track {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: var(--color-border);
  transition: background 0.2s ease;
}

.switch-track::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
  transition: transform 0.2s ease;
}

.switch input:checked + .switch-track {
  background: var(--color-secondary-500, #71904a);
}

.switch input:checked + .switch-track::after {
  transform: translateX(18px);
}

.switch input:focus-visible + .switch-track {
  outline: 2px solid var(--color-secondary-400);
  outline-offset: 2px;
}

/* Tema cards */
.tema-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.65rem;
}

.tema-card {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding: 0.65rem;
  border-radius: 14px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.tema-card:hover {
  border-color: var(--color-secondary-300, #9bb86a);
}

.tema-card.is-active {
  border-color: var(--color-secondary-500, #71904a);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-secondary-300) 35%, transparent);
}

.tema-preview {
  height: 52px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0.45rem;
  display: flex;
  gap: 0.35rem;
  overflow: hidden;
}

.tema-preview[data-preview='claro'] {
  background: #f6f1ea;
}

.tema-preview[data-preview='escuro'] {
  background: #1c1f18;
  border-color: #33382e;
}

.tema-preview[data-preview='sistema'] {
  background: linear-gradient(90deg, #f6f1ea 50%, #1c1f18 50%);
}

.tp-bar {
  width: 12px;
  border-radius: 4px;
  background: #71904a;
  opacity: 0.85;
}

.tema-preview[data-preview='escuro'] .tp-bar {
  background: #8fb36a;
}

.tp-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  justify-content: center;
}

.tp-lines i {
  display: block;
  height: 5px;
  border-radius: 3px;
  background: #c4b8a8;
}

.tema-preview[data-preview='escuro'] .tp-lines i {
  background: #4a5240;
}

.tema-preview[data-preview='sistema'] .tp-lines i {
  background: linear-gradient(90deg, #c4b8a8 50%, #4a5240 50%);
}

.tema-meta strong {
  display: block;
  font-size: 0.86rem;
  font-weight: 700;
}

.tema-meta small {
  display: block;
  font-size: 0.72rem;
  color: var(--color-text-muted);
  line-height: 1.3;
  margin-top: 0.1rem;
}

/* Guia */
.guia-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.guia-card {
  display: block;
  padding: 0.75rem 0.9rem;
  border-radius: 12px;
  border: 1.5px solid var(--color-border);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease,
    box-shadow 0.15s ease;
}

.guia-card:hover {
  border-color: var(--color-secondary-300, #9bb86a);
}

.guia-card.is-active {
  border-color: var(--color-secondary-500, #71904a);
  background: color-mix(in srgb, var(--color-secondary-100) 55%, var(--color-surface));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-secondary-300) 30%, transparent);
}

.guia-card strong {
  display: block;
  font-size: 0.9rem;
  font-weight: 700;
}

.guia-card small {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
  line-height: 1.35;
}

.def-footer {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding-top: 0.25rem;
}

.ok,
.erro {
  margin: 0;
  font-weight: 600;
  font-size: 0.88rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.ok {
  color: var(--color-secondary-700);
}

.erro {
  color: var(--color-danger, #b33);
}

.btn-guardar {
  border: none;
  border-radius: 999px;
  padding: 0.78rem 1.25rem;
  font-weight: 650;
  font-size: 0.95rem;
  background: var(--color-primary-500, #c4785a);
  color: #fff;
  cursor: pointer;
  font-family: inherit;
  box-shadow: 0 4px 14px color-mix(in srgb, var(--color-primary-500, #c4785a) 35%, transparent);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    opacity 0.15s ease;
}

.btn-guardar:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px color-mix(in srgb, var(--color-primary-500, #c4785a) 42%, transparent);
}

.btn-guardar:disabled {
  opacity: 0.7;
  cursor: wait;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

@media (max-width: 560px) {
  .tema-grid {
    grid-template-columns: 1fr;
  }
}
</style>
