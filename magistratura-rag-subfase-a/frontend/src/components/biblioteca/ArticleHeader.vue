<script setup>
import { BookMarked } from 'lucide-vue-next'
import VerificationBadge from './VerificationBadge.vue'

defineProps({
  artigo: { type: Object, required: true }
})
</script>

<template>
  <header class="article-header">
    <span class="eyebrow">{{ artigo.diplomaTitulo || 'Biblioteca Jurídica' }}</span>
    <div class="titulo-row">
      <BookMarked :size="22" class="icon" />
      <h1>
        <span class="numero">Art. {{ artigo.numero }}</span>
        <span v-if="artigo.titulo" class="epigrafe">{{ artigo.titulo }}</span>
      </h1>
    </div>
    <div class="meta-top">
      <span v-if="artigo.diplomaNumero">{{ artigo.diplomaNumero }}</span>
      <span v-if="artigo.diplomaCategoriaNome">{{ artigo.diplomaCategoriaNome }}</span>
      <VerificationBadge :oficial="artigo.documentoOficial" :estado="artigo.diplomaEstado" />
    </div>
  </header>
</template>

<style scoped>
.article-header {
  margin-bottom: 1.25rem;
}
.eyebrow {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-primary-700);
  background: var(--color-primary-100);
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-pill);
  margin-bottom: 0.75rem;
}
.titulo-row {
  display: flex;
  gap: 0.65rem;
  align-items: flex-start;
}
.icon {
  color: var(--color-secondary-500);
  margin-top: 0.35rem;
  flex-shrink: 0;
}
h1 {
  margin: 0;
  font-size: 1.55rem;
  line-height: 1.3;
}
.numero {
  display: block;
  color: var(--color-secondary-600);
}
.epigrafe {
  display: block;
  font-size: 1.05rem;
  font-weight: 500;
  color: var(--color-text-soft);
  margin-top: 0.25rem;
}
.meta-top {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.85rem;
  margin-top: 0.75rem;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  align-items: center;
}
</style>
