<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  artigos: { type: Array, default: () => [] },
  atualId: { type: String, default: null },
  capitulo: { type: String, default: null }
})

const router = useRouter()

const mesmoCapitulo = computed(() => {
  if (!props.capitulo) return []
  return props.artigos
    .filter((a) => a.id !== props.atualId && a.capitulo === props.capitulo)
    .slice(0, 8)
})

const doDiploma = computed(() =>
  props.artigos.filter((a) => a.id !== props.atualId).slice(0, 12)
)

function abrir(id) {
  router.push({ name: 'artigo', params: { id } })
}
</script>

<template>
  <div v-if="artigos.length" class="related">
    <section v-if="mesmoCapitulo.length" class="bloco">
      <h3>Mesmo capítulo</h3>
      <ul>
        <li v-for="a in mesmoCapitulo" :key="a.id">
          <button type="button" @click="abrir(a.id)">
            Art. {{ a.numero }}
            <span v-if="a.titulo">— {{ a.titulo }}</span>
          </button>
        </li>
      </ul>
    </section>
    <section class="bloco">
      <h3>Outros artigos deste diploma</h3>
      <ul>
        <li v-for="a in doDiploma" :key="'d-' + a.id">
          <button type="button" @click="abrir(a.id)">
            Art. {{ a.numero }}
            <span v-if="a.titulo">— {{ a.titulo }}</span>
          </button>
        </li>
      </ul>
      <p v-if="!doDiploma.length" class="vazio">Sem outros artigos listados.</p>
    </section>
  </div>
</template>

<style scoped>
.related {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-top: 1.5rem;
}
.bloco h3 {
  font-size: 0.9rem;
  margin: 0 0 0.55rem;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
button {
  background: transparent;
  border: none;
  text-align: left;
  color: var(--color-secondary-600);
  font-size: 0.85rem;
  cursor: pointer;
  padding: 0.35rem 0.2rem;
  border-radius: var(--radius-sm);
}
button:hover {
  background: var(--color-primary-50);
}
.vazio {
  font-size: 0.82rem;
  color: var(--color-text-muted);
  margin: 0;
}
</style>
