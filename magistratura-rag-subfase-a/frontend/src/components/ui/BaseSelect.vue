<script setup>
/**
 * Select customizado (sem lista azul do SO).
 * Painel em Teleport + position fixed para não ser cortado por overflow do card.
 */
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ChevronDown, Check, Search } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Seleccionar…' },
  disabled: { type: Boolean, default: false },
  searchable: { type: Boolean, default: false },
  searchPlaceholder: { type: String, default: 'Filtrar…' },
  emptyText: { type: String, default: 'Sem opções' }
})

const emit = defineEmits(['update:modelValue'])

const aberto = ref(false)
const root = ref(null)
const triggerRef = ref(null)
const panelRef = ref(null)
const listaRef = ref(null)
const filtro = ref('')
const destaque = ref(-1)
const panelStyle = ref({})

const seleccionado = computed(() =>
  props.options.find((o) => String(o.value) === String(props.modelValue ?? ''))
)
const rotulo = computed(() => seleccionado.value?.label || props.placeholder)

const filtradas = computed(() => {
  const lista = props.options || []
  if (!props.searchable || !filtro.value.trim()) return lista
  const t = filtro.value.trim().toLowerCase()
  return lista.filter((o) => String(o.label).toLowerCase().includes(t))
})

function posicionarPainel() {
  const el = triggerRef.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const minW = 280
  const width = Math.max(r.width, minW)
  let left = r.left
  // não sair da viewport à direita
  if (left + width > window.innerWidth - 12) {
    left = Math.max(12, window.innerWidth - width - 12)
  }
  const spaceBelow = window.innerHeight - r.bottom
  const preferBelow = spaceBelow >= 220 || spaceBelow >= r.top
  const maxH = Math.min(320, preferBelow ? spaceBelow - 12 : r.top - 12)

  panelStyle.value = {
    position: 'fixed',
    zIndex: 1000,
    width: `${width}px`,
    left: `${left}px`,
    maxHeight: `${Math.max(160, maxH)}px`,
    ...(preferBelow
      ? { top: `${r.bottom + 6}px`, bottom: 'auto' }
      : { bottom: `${window.innerHeight - r.top + 6}px`, top: 'auto' })
  }
}

watch(aberto, async (v) => {
  if (v) {
    filtro.value = ''
    const idx = filtradas.value.findIndex(
      (o) => String(o.value) === String(props.modelValue ?? '')
    )
    destaque.value = idx >= 0 ? idx : 0
    await nextTick()
    posicionarPainel()
    listaRef.value?.querySelector('.opcao.is-active')?.scrollIntoView({ block: 'nearest' })
  }
})

function alternar() {
  if (props.disabled) return
  aberto.value = !aberto.value
}

function escolher(opt) {
  emit('update:modelValue', opt.value)
  aberto.value = false
  filtro.value = ''
}

function limpar() {
  emit('update:modelValue', '')
  aberto.value = false
}

function onKey(e) {
  if (!aberto.value) {
    if ((e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') && !props.disabled) {
      e.preventDefault()
      aberto.value = true
    }
    return
  }
  const n = filtradas.value.length
  if (e.key === 'Escape') {
    e.preventDefault()
    aberto.value = false
    return
  }
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    destaque.value = n ? (destaque.value + 1) % n : -1
    return
  }
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    destaque.value = n ? (destaque.value - 1 + n) % n : -1
    return
  }
  if (e.key === 'Enter') {
    e.preventDefault()
    if (destaque.value >= 0 && filtradas.value[destaque.value]) {
      escolher(filtradas.value[destaque.value])
    }
  }
}

function onDoc(e) {
  const t = e.target
  if (root.value?.contains(t) || panelRef.value?.contains(t)) return
  aberto.value = false
}

function onScrollOrResize() {
  if (aberto.value) posicionarPainel()
}

onMounted(() => {
  document.addEventListener('mousedown', onDoc)
  window.addEventListener('resize', onScrollOrResize)
  window.addEventListener('scroll', onScrollOrResize, true)
})
onUnmounted(() => {
  document.removeEventListener('mousedown', onDoc)
  window.removeEventListener('resize', onScrollOrResize)
  window.removeEventListener('scroll', onScrollOrResize, true)
})
</script>

<template>
  <div
    ref="root"
    class="bs"
    :class="{ 'is-open': aberto, 'is-disabled': disabled, 'has-value': !!seleccionado }"
  >
    <button
      ref="triggerRef"
      type="button"
      class="bs-trigger"
      :disabled="disabled"
      :aria-expanded="aberto"
      aria-haspopup="listbox"
      @click="alternar"
      @keydown="onKey"
    >
      <span class="bs-label" :class="{ 'is-placeholder': !seleccionado }">{{ rotulo }}</span>
      <ChevronDown :size="18" class="bs-chevron" aria-hidden="true" />
    </button>

    <Teleport to="body">
      <Transition name="bs-drop">
        <div
          v-if="aberto"
          ref="panelRef"
          class="bs-panel"
          role="listbox"
          :style="panelStyle"
        >
          <div v-if="searchable" class="bs-search">
            <Search :size="15" aria-hidden="true" />
            <input
              v-model="filtro"
              type="search"
              :placeholder="searchPlaceholder"
              autocomplete="off"
              @keydown="onKey"
            />
          </div>

          <ul ref="listaRef" class="bs-lista">
            <li>
              <button
                type="button"
                class="opcao opcao-vazio"
                role="option"
                :aria-selected="!modelValue"
                @click="limpar"
              >
                <span>{{ placeholder }}</span>
                <Check v-if="!modelValue" :size="16" class="check" />
              </button>
            </li>
            <li v-for="(opt, i) in filtradas" :key="String(opt.value)">
              <button
                type="button"
                class="opcao"
                :class="{
                  'is-selected': String(opt.value) === String(modelValue ?? ''),
                  'is-active': i === destaque
                }"
                role="option"
                :aria-selected="String(opt.value) === String(modelValue ?? '')"
                @mouseenter="destaque = i"
                @click="escolher(opt)"
              >
                <span class="opcao-texto">{{ opt.label }}</span>
                <Check
                  v-if="String(opt.value) === String(modelValue ?? '')"
                  :size="16"
                  class="check"
                />
              </button>
            </li>
            <li v-if="!filtradas.length" class="bs-vazio">{{ emptyText }}</li>
          </ul>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.bs {
  position: relative;
  width: 100%;
  min-width: 0;
}

.bs-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  width: 100%;
  min-height: 44px;
  padding: 0.65rem 0.9rem;
  border-radius: var(--radius-sm, 8px);
  border: 1.5px solid var(--color-border, #ecdfd4);
  background: var(--color-surface-alt, #fdfaf7);
  color: var(--color-text, #10160f);
  font-family: var(--font-body, inherit);
  font-size: 0.95rem;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.bs-trigger:hover:not(:disabled) {
  border-color: var(--color-primary-300, #e7ae97);
  background: var(--color-surface, #fff);
}

.bs.is-open .bs-trigger,
.bs-trigger:focus-visible {
  border-color: var(--color-secondary-500, #71904a);
  box-shadow: 0 0 0 3px var(--color-secondary-100, #e7edd5);
  outline: none;
}

.bs-trigger:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.bs-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.35;
}

.bs-label.is-placeholder {
  color: var(--color-text-muted, #6b7568);
  font-weight: 400;
}

.bs-chevron {
  flex-shrink: 0;
  color: var(--color-secondary-600, #56732f);
  transition: transform 0.2s ease;
}

.bs.is-open .bs-chevron {
  transform: rotate(180deg);
}

.bs-drop-enter-active,
.bs-drop-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.bs-drop-enter-from,
.bs-drop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

<!-- painel no body: estilos não scoped -->
<style>
.bs-panel {
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #ecdfd4);
  border-radius: var(--radius-md, 14px);
  box-shadow: 0 12px 40px rgba(60, 45, 35, 0.14);
  overflow: hidden;
}

.bs-panel .bs-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid var(--color-border, #ecdfd4);
  color: var(--color-text-muted, #6b7568);
  background: var(--color-surface-alt, #fdfaf7);
  flex-shrink: 0;
}

.bs-panel .bs-search input {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 0.25rem 0 !important;
  font-size: 0.88rem;
  width: 100%;
  outline: none;
}

.bs-panel .bs-lista {
  list-style: none;
  margin: 0;
  padding: 0.4rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.bs-panel .opcao {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
  padding: 0.65rem 0.8rem;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text, #10160f);
  font-family: inherit;
  font-size: 0.9rem;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s ease, color 0.12s ease;
}

.bs-panel .opcao-texto {
  flex: 1;
  min-width: 0;
  /* permite 2 linhas em labels longos */
  white-space: normal;
  line-height: 1.35;
  word-break: break-word;
}

.bs-panel .opcao:hover,
.bs-panel .opcao.is-active {
  background: var(--color-secondary-100, #e7edd5);
  color: var(--color-secondary-700, #3f5622);
}

.bs-panel .opcao.is-selected {
  background: var(--color-secondary-100, #e7edd5);
  color: var(--color-secondary-700, #3f5622);
  font-weight: 600;
}

.bs-panel .opcao.is-selected.is-active,
.bs-panel .opcao.is-selected:hover {
  background: #dce6c8;
}

.bs-panel .opcao .check {
  flex-shrink: 0;
  color: var(--color-secondary-600, #56732f);
}

.bs-panel .opcao-vazio {
  color: var(--color-text-muted, #6b7568);
  font-weight: 400;
}

.bs-panel .bs-vazio {
  padding: 0.85rem 0.75rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7568);
  text-align: center;
}
</style>
