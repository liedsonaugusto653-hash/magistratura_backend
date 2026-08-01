<script setup>
defineProps({
  artigo: { type: Object, required: true }
})

function fmtData(v) {
  if (!v) return '—'
  return String(v).slice(0, 10)
}
</script>

<template>
  <dl class="meta-grid">
    <div v-if="artigo.capitulo">
      <dt>Capítulo</dt>
      <dd>{{ artigo.capitulo }}</dd>
    </div>
    <div v-if="artigo.seccao">
      <dt>Secção</dt>
      <dd>{{ artigo.seccao }}</dd>
    </div>
    <div>
      <dt>Diploma</dt>
      <dd>{{ artigo.diplomaTitulo || '—' }}</dd>
    </div>
    <div>
      <dt>Tipo / categoria</dt>
      <dd>{{ artigo.diplomaCategoriaNome || '—' }}</dd>
    </div>
    <div>
      <dt>Data de publicação</dt>
      <dd>{{ fmtData(artigo.diplomaDataPublicacao) }}</dd>
    </div>
    <div>
      <dt>Versão do diploma</dt>
      <dd>{{ artigo.diplomaVersao ?? '—' }}</dd>
    </div>
    <div>
      <dt>Fonte do documento</dt>
      <dd>{{ artigo.documentoFonte || artigo.documentoTitulo || '—' }}</dd>
    </div>
    <div>
      <dt>Página(s) no PDF</dt>
      <dd>
        <template v-if="artigo.paginaInicio">
          {{ artigo.paginaInicio
          }}<template v-if="artigo.paginaFim && artigo.paginaFim !== artigo.paginaInicio"
            >–{{ artigo.paginaFim }}</template
          >
        </template>
        <template v-else>—</template>
      </dd>
    </div>
    <div>
      <dt>Importado em</dt>
      <dd>{{ fmtData(artigo.documentoDataImportacao) }}</dd>
    </div>
    <div>
      <dt>Identificador</dt>
      <dd class="id">{{ artigo.id }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.85rem 1.25rem;
  margin: 0 0 1.5rem;
  padding: 1rem 1.15rem;
  background: var(--color-surface-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
dt {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  margin: 0 0 0.2rem;
}
dd {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text);
}
.id {
  font-family: ui-monospace, monospace;
  font-size: 0.72rem;
  word-break: break-all;
  color: var(--color-text-muted);
}
</style>
