<script setup>
defineProps({
  message: { type: String, default: 'A carregar…' }
})
</script>
<template>
  <div class="center-state" role="status" aria-live="polite">
    <div class="spinner" aria-hidden="true"></div>
    <p>{{ message }}</p>
  </div>
</template>
