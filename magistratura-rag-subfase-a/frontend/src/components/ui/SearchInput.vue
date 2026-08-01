<script setup>
import { Search } from 'lucide-vue-next'

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Pesquisar…' },
  loading: { type: Boolean, default: false },
  hint: { type: String, default: '' }
})
defineEmits(['update:modelValue', 'search', 'clear'])
</script>

<template>
  <div class="search-wrap">
    <div class="search-bar">
      <Search :size="18" class="search-icon" />
      <input
        class="search-input"
        :value="modelValue"
        :placeholder="placeholder"
        @input="$emit('update:modelValue', $event.target.value)"
        @keyup.enter="$emit('search')"
      />
      <button
        v-if="modelValue"
        type="button"
        class="btn-clear"
        @click="$emit('update:modelValue', ''); $emit('clear')"
      >
        Limpar
      </button>
      <button class="btn btn-primary" type="button" :disabled="loading" @click="$emit('search')">
        {{ loading ? 'A pesquisar…' : 'Pesquisar' }}
      </button>
    </div>
    <p v-if="hint" class="search-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.search-wrap {
  width: 100%;
  max-width: 100%;
  margin: 0;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.35rem 0.35rem 0.35rem 0.85rem;
  transition: border-color 0.15s ease;
}
.search-bar:focus-within {
  border-color: var(--color-secondary-300);
}
.search-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.search-input {
  flex: 1;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 0.55rem 0.25rem;
  font-size: 0.92rem;
  outline: none;
  width: 100%;
}
.btn-clear {
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0.35rem 0.5rem;
}
.btn-clear:hover {
  color: var(--color-danger);
}
.search-hint {
  margin: 0.45rem 0 0;
  font-size: 0.76rem;
  color: var(--color-text-muted);
  text-align: center;
}
</style>
