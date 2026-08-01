<script setup>
defineProps({
  message: { type: String, default: 'Não foi possível carregar os dados.' },
  retryLabel: { type: String, default: 'Tentar novamente' },
  showRetry: { type: Boolean, default: true }
})
defineEmits(['retry'])
</script>

<template>
  <div class="center-state error-state">
    <div class="error-badge" aria-hidden="true">
      <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
        <circle cx="24" cy="24" r="18" stroke="currentColor" stroke-width="2" fill="var(--color-primary-50)" />
        <path d="M24 16v12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
        <circle cx="24" cy="34" r="1.8" fill="currentColor" />
      </svg>
    </div>
    <p>{{ message }}</p>
    <button v-if="showRetry" type="button" class="btn btn-secondary" @click="$emit('retry')">
      {{ retryLabel }}
    </button>
    <slot />
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem 1rem;
}
.error-badge {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #fbeee7, #f6dad0);
  color: var(--color-danger);
}
.error-state p {
  color: var(--color-danger);
  margin: 0;
  max-width: 360px;
  text-align: center;
  font-size: 0.92rem;
  line-height: 1.45;
}
</style>
