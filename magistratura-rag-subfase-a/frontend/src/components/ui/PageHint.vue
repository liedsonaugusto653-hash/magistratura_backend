<script setup>
import { useUiStore } from '@/stores/ui'
defineProps({ text: { type: String, default: '' } })
const ui = useUiStore()
</script>
<template>
  <p v-if="ui.mostrarDicas && text" class="page-hint">{{ text }}</p>
</template>
<style scoped>
.page-hint {
  margin: 0 0 1rem;
  font-size: 0.85rem;
  line-height: 1.45;
  color: var(--color-text-muted);
  padding: 0.65rem 0.85rem;
  border-radius: 10px;
  background: var(--color-secondary-50, #f4faf6);
  border: 1px solid var(--color-secondary-100, #e8f2eb);
}
</style>
