<script setup>
defineProps({
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  htmlFor: { type: String, default: '' }
})
</script>
<template>
  <div class="field" :class="{ 'has-error': !!error }">
    <label v-if="label" :for="htmlFor">{{ label }}</label>
    <slot />
    <small v-if="error" class="field-error" role="alert">{{ error }}</small>
    <small v-else-if="hint">{{ hint }}</small>
  </div>
</template>
<style scoped>
.has-error :deep(input),
.has-error :deep(select),
.has-error :deep(textarea) {
  border-color: var(--color-danger);
}
.field-error { color: var(--color-danger); }
</style>
