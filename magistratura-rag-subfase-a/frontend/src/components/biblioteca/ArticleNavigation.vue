<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

defineProps({
  anteriorId: { type: String, default: null },
  seguinteId: { type: String, default: null }
})
defineEmits(['anterior', 'seguinte'])
</script>

<template>
  <nav class="nav-artigos">
    <button class="btn btn-ghost" :disabled="!anteriorId" @click="$emit('anterior')">
      <ChevronLeft :size="16" /> Artigo anterior
    </button>
    <button class="btn btn-ghost" :disabled="!seguinteId" @click="$emit('seguinte')">
      Artigo seguinte <ChevronRight :size="16" />
    </button>
  </nav>
</template>

<style scoped>
.nav-artigos {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin: 1.25rem 0;
  flex-wrap: wrap;
}
</style>
