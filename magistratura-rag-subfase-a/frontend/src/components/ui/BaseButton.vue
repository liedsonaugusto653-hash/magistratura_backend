<script setup>
/**
 * Variantes: primary | secondary | ghost | danger | accent
 * loading: mostra texto de loading e desativa o botão
 */
defineProps({
  variant: { type: String, default: 'primary' },
  type: { type: String, default: 'button' },
  loading: { type: Boolean, default: false },
  loadingText: { type: String, default: 'A processar…' },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false }
})
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[
      'btn-' + variant,
      { 'btn-block': block }
    ]"
    :disabled="disabled || loading"
  >
    <slot v-if="!loading" />
    <template v-else>{{ loadingText }}</template>
  </button>
</template>
