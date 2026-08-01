<script setup>
import OuvirTexto from '@/components/audio/OuvirTexto.vue'

defineProps({
  texto: { type: String, default: '' }
})
</script>

<template>
  <article class="article-content card">
    <div class="cabecalho">
      <span class="label">Texto oficial</span>
    </div>

    <OuvirTexto
      v-if="texto"
      :texto="texto"
      label="Ouvir texto oficial"
      :com-destaque="true"
    />

    <!-- Fallback visual se TTS não mostrar destaque (texto vazio já tratado) -->
    <div v-if="!texto" class="texto vazio">Sem texto disponível para este artigo.</div>

    <p class="aviso">
      Texto extraído do documento de origem. Não foi reescrito nem gerado por inteligência
      artificial.
    </p>
  </article>
</template>

<style scoped>
.article-content {
  margin-bottom: 1.25rem;
}
.cabecalho {
  margin-bottom: 0.65rem;
}
.label {
  display: block;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-secondary-600);
}
.texto.vazio {
  font-size: 0.95rem;
  color: var(--color-text-muted);
  padding: 0.5rem 0;
}
.aviso {
  margin: 1.1rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
</style>
