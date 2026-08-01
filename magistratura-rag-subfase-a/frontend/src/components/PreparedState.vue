<script setup>
import { PackageOpen } from 'lucide-vue-next'

defineProps({
  titulo: { type: String, required: true },
  descricao: { type: String, required: true },
  endpoint: { type: String, default: '' }
})
</script>

<template>
  <div class="prepared card card-tint-primary">
    <div class="icon">
      <PackageOpen :size="26" />
    </div>
    <span class="badge badge-soon">Aguarda endpoint no backend</span>
    <h3>{{ titulo }}</h3>
    <p>{{ descricao }}</p>
    <code v-if="endpoint">{{ endpoint }}</code>
    <p class="hint">
      A camada de serviço já está pronta no frontend — assim que este endpoint existir na API,
      esta secção passa a mostrar dados reais automaticamente, sem dados inventados.
    </p>
  </div>
</template>

<style scoped>
.prepared {
  max-width: 560px;
  margin: 0 auto;
  text-align: center;
}

.icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 0.9rem;
}

.badge {
  margin-bottom: 0.7rem;
}

code {
  display: inline-block;
  background: var(--color-secondary-100);
  color: var(--color-secondary-700);
  padding: 0.3rem 0.7rem;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  margin-bottom: 0.8rem;
}

.hint {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
