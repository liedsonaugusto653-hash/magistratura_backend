<script setup>
/**
 * Visualizador PDF embutido (iframe + blob URL).
 * Carrega o blob só quando o painel entra no viewport (poupa rede no first paint).
 */
import { ref, watch, computed, onBeforeUnmount, onMounted } from 'vue'
import documentoService from '@/services/documentoService'
import { FileText, ExternalLink, AlertTriangle } from 'lucide-vue-next'

const props = defineProps({
  documentoId: { type: String, default: null },
  pagina: { type: Number, default: null },
  paginaFim: { type: Number, default: null },
  rotulo: { type: String, default: '' }
})

const rootEl = ref(null)
const visivel = ref(false)
const objectUrlBase = ref(null)
const erro = ref('')
const aCarregar = ref(false)
const iframeKey = ref(0)
let observer = null

const iframeSrc = computed(() => {
  if (!objectUrlBase.value) return null
  if (props.pagina && props.pagina > 0) {
    return `${objectUrlBase.value}#page=${props.pagina}&zoom=page-width`
  }
  return objectUrlBase.value
})

const intervaloPaginas = computed(() => {
  if (!props.pagina) return null
  if (props.paginaFim && props.paginaFim !== props.pagina) {
    return `pp. ${props.pagina}–${props.paginaFim}`
  }
  return `p. ${props.pagina}`
})

async function carregar() {
  erro.value = ''
  if (objectUrlBase.value) {
    URL.revokeObjectURL(objectUrlBase.value)
    objectUrlBase.value = null
  }
  if (!props.documentoId || !visivel.value) return
  aCarregar.value = true
  try {
    const blob = await documentoService.obterPdfBlob(props.documentoId)
    objectUrlBase.value = URL.createObjectURL(blob)
    iframeKey.value += 1
  } catch {
    erro.value = 'Não foi possível carregar o PDF de origem.'
  } finally {
    aCarregar.value = false
  }
}

onMounted(() => {
  if (typeof IntersectionObserver === 'undefined') {
    visivel.value = true
    carregar()
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      const e = entries[0]
      if (e?.isIntersecting) {
        visivel.value = true
        carregar()
        observer?.disconnect()
        observer = null
      }
    },
    { root: null, rootMargin: '120px', threshold: 0.01 }
  )
  if (rootEl.value) observer.observe(rootEl.value)
})

watch(
  () => props.documentoId,
  () => {
    if (visivel.value) carregar()
  }
)

watch(
  () => props.pagina,
  () => {
    if (objectUrlBase.value) iframeKey.value += 1
  }
)

onBeforeUnmount(() => {
  observer?.disconnect()
  if (objectUrlBase.value) URL.revokeObjectURL(objectUrlBase.value)
})

function abrirSeparador() {
  if (iframeSrc.value) window.open(iframeSrc.value, '_blank', 'noopener')
}
</script>
<template>
  <aside ref="rootEl" class="pdf-preview">
    <header class="pdf-toolbar">
      <div class="pdf-toolbar-left">
        <FileText :size="16" class="pdf-ico" />
        <div class="pdf-titles">
          <span class="pdf-label">Documento de origem</span>
          <span v-if="rotulo" class="pdf-rotulo">{{ rotulo }}</span>
        </div>
      </div>
      <div class="pdf-toolbar-right">
        <span v-if="intervaloPaginas" class="pdf-pagina">{{ intervaloPaginas }}</span>
        <button
          v-if="iframeSrc"
          type="button"
          class="pdf-btn"
          title="Abrir em separador"
          @click="abrirSeparador"
        >
          <ExternalLink :size="15" />
        </button>
      </div>
    </header>

    <p class="pdf-hint">
      Página do PDF oficial associada a este artigo. O texto à esquerda é a extracção estruturada.
    </p>

    <div class="pdf-viewport">
      <div v-if="!documentoId" class="estado">
        <FileText :size="28" stroke-width="1.25" />
        <p>Sem documento PDF associado a este artigo.</p>
      </div>
      <div v-else-if="aCarregar" class="estado">
        <div class="spinner" />
        <p>A carregar o documento…</p>
      </div>
      <div v-else-if="erro" class="estado erro">
        <AlertTriangle :size="22" />
        <p>{{ erro }}</p>
        <button type="button" class="pdf-btn-texto" @click="carregar">Tentar novamente</button>
      </div>
      <iframe
        v-else-if="iframeSrc"
        :key="iframeKey"
        :src="iframeSrc"
        title="PDF de origem"
        class="frame"
      />
    </div>
  </aside>
</template>

<style scoped>
.pdf-preview {
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 1rem;
  background: #f6f4f0;
  border: 1px solid #ddd8d0;
  border-radius: 12px;
  overflow: hidden;
  min-height: 520px;
  box-shadow: 0 2px 12px rgba(40, 35, 25, 0.06);
}
.pdf-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.7rem 0.85rem;
  background: #2c3330;
  color: #e8ebe9;
}
.pdf-toolbar-left {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}
.pdf-ico { flex-shrink: 0; opacity: 0.85; }
.pdf-titles {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}
.pdf-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  opacity: 0.75;
}
.pdf-rotulo {
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pdf-toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
}
.pdf-pagina {
  font-size: 0.72rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.12);
}
.pdf-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: #e8ebe9;
  cursor: pointer;
}
.pdf-btn:hover { background: rgba(255, 255, 255, 0.1); }
.pdf-hint {
  margin: 0;
  padding: 0.45rem 0.85rem;
  font-size: 0.72rem;
  line-height: 1.4;
  color: #6a6560;
  background: #efece6;
  border-bottom: 1px solid #ddd8d0;
}
.pdf-viewport {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 420px;
  background: #e5e1da;
  padding: 0.65rem;
}
.frame {
  flex: 1;
  width: 100%;
  min-height: 420px;
  border: 1px solid #cfc9c0;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(40, 35, 25, 0.08);
}
.estado {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  color: #6a6560;
  font-size: 0.85rem;
  text-align: center;
  padding: 2rem 1.25rem;
}
.estado p { margin: 0; max-width: 16rem; line-height: 1.4; }
.estado.erro { color: #a94442; }
.pdf-btn-texto {
  font-family: inherit;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  border: 1px solid #cfc9c0;
  background: #fff;
  color: #3d3a36;
  cursor: pointer;
}
.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #cfc9c0;
  border-top-color: #3d6b4f;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 960px) {
  .pdf-preview {
    position: relative;
    top: auto;
    min-height: 360px;
    margin-top: 0.5rem;
  }
  .pdf-viewport {
    min-height: 280px;
  }
  .frame {
    min-height: 280px;
  }
}
@media (max-width: 560px) {
  .pdf-preview {
    min-height: 300px;
    border-radius: 10px;
  }
  .pdf-toolbar {
    padding: 0.55rem 0.65rem;
  }
  .pdf-viewport {
    padding: 0.4rem;
    min-height: 240px;
  }
  .frame {
    min-height: 240px;
  }
}
</style>
