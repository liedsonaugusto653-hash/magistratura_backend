<script setup>
defineProps({
  oficial: { type: Boolean, default: null },
  estado: { type: String, default: '' }
})
</script>

<template>
  <span
    v-if="oficial === true"
    class="badge-verif verificado"
    title="Documento marcado como fonte oficial"
  >
    Verificado
  </span>
  <span
    v-else-if="oficial === false"
    class="badge-verif nao-verificado"
    title="Documento ainda não validado como fonte oficial"
  >
    Não verificado
  </span>
  <span v-else-if="estado" class="badge-verif neutro">{{ estado }}</span>
</template>

<style scoped>
.badge-verif {
  display: inline-flex;
  align-items: center;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0.28rem 0.65rem;
  border-radius: var(--radius-pill);
}
.verificado {
  background: var(--color-accent-100);
  color: var(--color-accent-600);
}
.nao-verificado {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.neutro {
  background: var(--color-secondary-100);
  color: var(--color-secondary-600);
}
</style>
