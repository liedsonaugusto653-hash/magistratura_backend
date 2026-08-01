<script setup>
import { useRouter } from 'vue-router'
import BrandMark from '@/components/brand/BrandMark.vue'
const router = useRouter()
</script>
<template>
  <div class="not-found">
    <BrandMark :size="72" />
    <h1>Página não encontrada</h1>
    <p>Este caminho não existe na plataforma.</p>
    <button type="button" class="btn btn-primary" @click="router.push('/')">Ir ao painel</button>
  </div>
</template>
<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  text-align: center;
  padding: 2rem;
}
.not-found h1 { margin: 0.5rem 0 0; font-size: 1.5rem; }
.not-found p { color: var(--color-text-muted); margin: 0 0 1rem; }
</style>
