<script setup>
import { onMounted, onUnmounted } from 'vue'
import { X } from 'lucide-vue-next'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' } // sm | md
})
const emit = defineEmits(['close'])

function onKey(e) {
  if (e.key === 'Escape' && props.open) emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-overlay" role="presentation" @click.self="emit('close')">
      <div
        class="modal-panel"
        :class="'modal-' + size"
        role="dialog"
        aria-modal="true"
        :aria-label="title || 'Diálogo'"
      >
        <header v-if="title || $slots.header" class="modal-header">
          <h2>{{ title }}</h2>
          <button type="button" class="modal-close" aria-label="Fechar" @click="emit('close')">
            <X :size="18" />
          </button>
        </header>
        <div class="modal-body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="modal-footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(16, 22, 15, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}
.modal-panel {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-lg);
  width: 100%;
  max-height: 90vh;
  overflow: auto;
}
.modal-md { max-width: 440px; }
.modal-sm { max-width: 360px; }
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem 0.5rem;
}
.modal-header h2 {
  font-size: 1.1rem;
  margin: 0;
}
.modal-close {
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.35rem;
  border-radius: var(--radius-sm);
}
.modal-close:hover { color: var(--color-danger); background: #f8e4e0; }
.modal-body { padding: 0.75rem 1.25rem 1rem; color: var(--color-text-soft); font-size: 0.92rem; }
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
  padding: 0 1.25rem 1.25rem;
}
</style>
