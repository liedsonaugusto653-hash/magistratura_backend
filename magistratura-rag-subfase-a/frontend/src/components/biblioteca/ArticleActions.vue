<script setup>
import { MessageSquare, Sparkles, Layers, ListChecks, Star } from 'lucide-vue-next'

defineProps({
  favorito: { type: Boolean, default: false },
  aFavoritar: { type: Boolean, default: false },
  /** Bloqueia botões de geração enquanto a IA processa */
  gerandoFlashcards: { type: Boolean, default: false },
  gerandoQuestoes: { type: Boolean, default: false }
})
defineEmits(['perguntar', 'explicar', 'exemplos', 'flashcards', 'questoes', 'favorito'])
</script>

<template>
  <div class="acoes card">
    <h3>Ações de estudo</h3>
    <div class="grid-acoes">
      <button class="btn btn-primary" type="button" @click="$emit('perguntar')">
        <MessageSquare :size="16" /> Perguntar ao Tutor
      </button>
      <button class="btn btn-secondary" type="button" @click="$emit('explicar')">
        <Sparkles :size="16" /> Explicar em linguagem simples
      </button>
      <button class="btn btn-ghost" type="button" @click="$emit('exemplos')">
        <Sparkles :size="16" /> Exemplos práticos
      </button>
      <button
        class="btn btn-ghost"
        type="button"
        :disabled="gerandoFlashcards || gerandoQuestoes"
        @click="$emit('flashcards')"
      >
        <Layers :size="16" />
        {{ gerandoFlashcards ? '⏳ A criar flashcards…' : '✨ Gerar flashcards com IA' }}
      </button>
      <button
        class="btn btn-ghost"
        type="button"
        :disabled="gerandoFlashcards || gerandoQuestoes"
        @click="$emit('questoes')"
      >
        <ListChecks :size="16" />
        {{ gerandoQuestoes ? '⏳ A criar questões…' : '✨ Gerar questões com IA' }}
      </button>
      <button
        class="btn btn-ghost"
        type="button"
        :class="{ ativo: favorito }"
        :disabled="aFavoritar"
        @click="$emit('favorito')"
      >
        <Star :size="16" :fill="favorito ? 'currentColor' : 'none'" />
        {{ favorito ? 'Nos favoritos' : 'Adicionar aos favoritos' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.acoes h3 {
  margin: 0 0 0.85rem;
  font-size: 0.95rem;
}
.grid-acoes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}
.ativo {
  border-color: var(--color-accent-500) !important;
  color: var(--color-accent-600) !important;
  background: var(--color-accent-100) !important;
}
</style>
