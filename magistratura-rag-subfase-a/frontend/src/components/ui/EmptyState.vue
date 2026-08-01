<script setup>
/**
 * Estado vazio orientador — ilustração + texto.
 */
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  icon: { type: [Object, Function], default: null }
})
</script>

<template>
  <div class="empty-state card card-static">
    <div class="empty-icon">
      <slot name="icon">
        <component :is="icon" v-if="icon" :size="28" />
        <svg v-else width="40" height="40" viewBox="0 0 48 48" fill="none" aria-hidden="true">
          <rect x="8" y="12" width="32" height="24" rx="4" stroke="currentColor" stroke-width="1.8" fill="none" opacity="0.35" />
          <path d="M16 22h16M16 28h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" opacity="0.5" />
          <circle cx="36" cy="34" r="8" fill="var(--color-primary-100)" stroke="currentColor" stroke-width="1.5" />
          <path d="M36 31v6M33 34h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
      </slot>
    </div>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  max-width: 440px;
  margin: 1.5rem auto;
  text-align: center;
  padding: var(--space-xl) var(--space-lg);
}
.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 1rem;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, var(--color-primary-100), var(--color-secondary-100));
  color: var(--color-secondary-700);
}
.empty-state h3 {
  margin: 0 0 0.5rem;
  font-size: 1.1rem;
}
.empty-state p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-text-muted);
  line-height: 1.5;
}
.empty-actions {
  margin-top: 1.25rem;
  display: flex;
  justify-content: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}
</style>
