<script setup>
/**
 * variant: neutral | live | soon | off | success | warning | danger | accent
 */
defineProps({
  variant: { type: String, default: 'neutral' },
  label: { type: String, default: '' }
})
</script>

<template>
  <span class="badge" :class="'badge-' + variant">
    <slot>{{ label }}</slot>
  </span>
</template>

<style scoped>
.badge-neutral {
  background: var(--color-surface-alt);
  color: var(--color-text-soft);
  border: 1px solid var(--color-border);
}
.badge-success {
  background: #e8f5e6;
  color: var(--color-success);
}
.badge-warning {
  background: #fbf1e2;
  color: var(--color-warning);
}
.badge-danger {
  background: #fbeae6;
  color: var(--color-danger);
}
.badge-accent {
  background: var(--color-accent-100);
  color: var(--color-accent-600);
}
/* live / soon / off já existem em main.css */
</style>
