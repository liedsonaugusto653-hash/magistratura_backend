<script setup>
import PageHint from '@/components/ui/PageHint.vue'
import { confirmarEliminacao } from '@/utils/prefsUi'
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBibliotecaStore } from '@/stores/biblioteca'
import bibliotecaService from '@/services/bibliotecaService'
import { BookMarked, FileText, Plus, Pencil, Trash2, ChevronRight, ArrowLeft } from 'lucide-vue-next'
import { SearchInput, LoadingState, ErrorState, EmptyState, BaseButton, BaseSelect } from '@/components/ui'
import { emitGuideEvent, GuideEvent } from '@/guide/events'
import { PageHero } from '@/components/ui'
import FeatureArt from '@/components/brand/FeatureArt.vue'

const store = useBibliotecaStore()
const route = useRoute()
const router = useRouter()
const termo = ref('')
const diplomaSelecionado = ref(null)
const artigosEncontrados = ref([])
const aPesquisarArtigos = ref(false)
const pesquisou = ref(false)
const erroPesquisa = ref('')

const modoFormDiploma = ref(false)
const editandoDiplomaId = ref(null)
const formDiplomaErro = ref('')
const formDiploma = reactive({
  numero: '',
  titulo: '',
  descricao: '',
  dataPublicacao: '',
  categoriaId: ''
})

const temTermo = computed(() => termo.value.trim().length > 0)
const semResultados = computed(() => {
  return (
    pesquisou.value &&
    !store.carregando &&
    !aPesquisarArtigos.value &&
    !store.diplomas.length &&
    !artigosEncontrados.value.length
  )
})

onMounted(async () => {
  await store.carregarCategorias()
  await store.carregarDiplomas()
  if (route.query.diplomaId) {
    await abrirDiploma(route.query.diplomaId)
  }
  if (route.query.termo) {
    termo.value = String(route.query.termo)
    await pesquisar()
  }
})

async function pesquisar() {
  diplomaSelecionado.value = null
  erroPesquisa.value = ''
  pesquisou.value = true
  const t = termo.value.trim()
  await store.carregarDiplomas({ termo: t || undefined })
  artigosEncontrados.value = []
  if (t) {
    aPesquisarArtigos.value = true
    try {
      const data = await bibliotecaService.listarArtigos({ termo: t, size: 50 })
      artigosEncontrados.value = data.content || data || []
    } catch {
      artigosEncontrados.value = []
      erroPesquisa.value = 'Não foi possível pesquisar artigos. Tenta novamente.'
    } finally {
      aPesquisarArtigos.value = false
    }
  }
  // Guia: pesquisa sem resultados (diplomas + artigos)
  try {
    const diplomas = store.diplomas?.length || store.lista?.length || 0
    const artigos = artigosEncontrados.value?.length || 0
    if (t && diplomas === 0 && artigos === 0 && !erroPesquisa.value) {
      emitGuideEvent(GuideEvent.EMPTY_SEARCH, { termo: t })
    }
  } catch {
    /* ignore */
  }
}

async function abrirDiploma(id) {
  erroPesquisa.value = ''
  try {
    await store.obterDiploma(id)
    diplomaSelecionado.value = store.diplomaAtual
    artigosEncontrados.value = []
  } catch {
    erroPesquisa.value = 'Não foi possível carregar o diploma.'
  }
}

function voltar() {
  diplomaSelecionado.value = null
  router.replace({ name: 'biblioteca' })
}

function abrirArtigo(id) {
  router.push({ name: 'artigo', params: { id } })
}

function limparPesquisa() {
  termo.value = ''
  pesquisou.value = false
  artigosEncontrados.value = []
  erroPesquisa.value = ''
  store.carregarDiplomas()
}


const opcoesCategorias = computed(() =>
  (store.categorias || []).map((c) => ({ value: c.id, label: c.nome }))
)

function abrirNovoDiploma() {
  editandoDiplomaId.value = null
  formDiploma.numero = ''
  formDiploma.titulo = ''
  formDiploma.descricao = ''
  formDiploma.dataPublicacao = ''
  formDiploma.categoriaId = ''
  formDiplomaErro.value = ''
  modoFormDiploma.value = true
  diplomaSelecionado.value = null
}

function abrirEditarDiploma() {
  const d = diplomaSelecionado.value
  if (!d) return
  editandoDiplomaId.value = d.id
  formDiploma.numero = d.numero || ''
  formDiploma.titulo = d.titulo || ''
  formDiploma.descricao = d.descricao || ''
  formDiploma.dataPublicacao = d.dataPublicacao ? String(d.dataPublicacao).slice(0, 10) : ''
  formDiploma.categoriaId = d.categoriaId || ''
  formDiplomaErro.value = ''
  modoFormDiploma.value = true
}

function cancelarFormDiploma() {
  modoFormDiploma.value = false
  formDiplomaErro.value = ''
}

async function guardarDiploma() {
  formDiplomaErro.value = ''
  if (!formDiploma.numero.trim() || !formDiploma.titulo.trim()) {
    formDiplomaErro.value = 'Número e título são obrigatórios.'
    return
  }
  const payload = {
    numero: formDiploma.numero.trim(),
    titulo: formDiploma.titulo.trim(),
    descricao: formDiploma.descricao.trim() || undefined,
    dataPublicacao: formDiploma.dataPublicacao || undefined,
    categoriaId: formDiploma.categoriaId || undefined
  }
  try {
    if (editandoDiplomaId.value) {
      const actualizado = await store.actualizarDiploma(editandoDiplomaId.value, payload)
      diplomaSelecionado.value = actualizado
    } else {
      const criado = await store.criarDiploma(payload)
      diplomaSelecionado.value = criado
    }
    modoFormDiploma.value = false
    await store.carregarDiplomas()
  } catch (_) {
    formDiplomaErro.value = store.erro || 'Não foi possível guardar o diploma.'
  }
}

async function eliminarDiplomaActual() {
  const d = diplomaSelecionado.value
  if (!d) return
  const nArtigos = d.artigos?.length || 0
  const msg =
    nArtigos > 0
      ? `Eliminar «${d.titulo}» e os seus ${nArtigos} artigo(s)? Esta acção não pode ser anulada.`
      : `Eliminar o diploma «${d.titulo}»?`
  if (!confirmarEliminacao(msg)) return
  try {
    await store.eliminarDiploma(d.id)
    diplomaSelecionado.value = null
    router.replace({ name: 'biblioteca' })
  } catch (_) {
    erroPesquisa.value = store.erro || 'Não foi possível eliminar o diploma.'
  }
}

function recarregar() {
  erroPesquisa.value = ''
  if (diplomaSelecionado.value?.id) {
    abrirDiploma(diplomaSelecionado.value.id)
  } else {
    pesquisar()
  }
}
</script>

<template>
  <div class="page bib-page">
    <PageHero
      eyebrow="Biblioteca"
      title="Biblioteca Jurídica"
      lead="Diplomas, artigos e material de estudo — a base para compreender a lei."
      art="biblioteca"
    >
      <template #actions>
        <BaseButton variant="primary" @click="abrirNovoDiploma">
          <Plus :size="16" />
          Novo diploma
        </BaseButton>
        <BaseButton variant="secondary" @click="$router.push('/documentos')">
          Importar PDF
        </BaseButton>
      </template>
    </PageHero>

    <PageHint
      text="Pesquisa por número, título ou palavra do artigo. Abre um diploma para ler a estrutura completa."
    />

    <!-- Barra de pesquisa -->
    <div class="toolbar">
      <div class="toolbar-search">
        <SearchInput
          v-model="termo"
          placeholder="Pesquisar diplomas ou artigos…"
          @submit="pesquisar"
          @clear="limparPesquisa"
        />
      </div>
      <BaseButton variant="primary" @click="pesquisar">Pesquisar</BaseButton>
    </div>

    <LoadingState v-if="store.carregando && !diplomaSelecionado && !modoFormDiploma" message="A carregar a biblioteca…" />
    <ErrorState
      v-else-if="store.erro && !diplomaSelecionado && !modoFormDiploma"
      :message="store.erro"
      @retry="recarregar"
    />

    <!-- Formulário diploma -->
    <section v-else-if="modoFormDiploma" class="panel form-panel">
      <header class="panel-head">
        <FeatureArt variant="biblioteca" :size="36" :animated="false" />
        <div>
          <h2>{{ editandoDiplomaId ? 'Editar diploma' : 'Novo diploma' }}</h2>
          <p>Metadados do diploma na biblioteca.</p>
        </div>
      </header>
      <div class="form-grid">
        <label class="field">
          <span>Número</span>
          <input v-model="formDiploma.numero" type="text" required placeholder="ex.: Lei n.º …" />
        </label>
        <label class="field">
          <span>Data de publicação</span>
          <input v-model="formDiploma.dataPublicacao" type="date" />
        </label>
        <label class="field field-full">
          <span>Título</span>
          <input v-model="formDiploma.titulo" type="text" required />
        </label>
        <label class="field field-full">
          <span>Descrição</span>
          <textarea v-model="formDiploma.descricao" rows="3" />
        </label>
        <label class="field field-full">
          <span>Categoria</span>
          <BaseSelect v-model="formDiploma.categoriaId" :options="opcoesCategorias" placeholder="Seleccionar…" />
        </label>
      </div>
      <p v-if="formDiplomaErro" class="form-erro">{{ formDiplomaErro }}</p>
      <div class="form-actions">
        <BaseButton variant="secondary" type="button" @click="cancelarFormDiploma">Cancelar</BaseButton>
        <BaseButton variant="primary" type="button" @click="guardarDiploma">Guardar</BaseButton>
      </div>
    </section>

    <!-- Detalhe diploma -->
    <section v-else-if="diplomaSelecionado" class="panel diploma-panel">
      <button type="button" class="back-link" @click="voltar">
        <ArrowLeft :size="16" />
        Voltar à lista
      </button>

      <header class="diploma-hero">
        <div class="diploma-hero-art">
          <FeatureArt variant="biblioteca" :size="48" :animated="false" />
        </div>
        <div class="diploma-hero-copy">
          <div class="diploma-chips">
            <span v-if="diplomaSelecionado.numero" class="chip">{{ diplomaSelecionado.numero }}</span>
            <span v-if="diplomaSelecionado.categoriaNome" class="chip chip-muted">{{ diplomaSelecionado.categoriaNome }}</span>
            <span v-if="diplomaSelecionado.estado" class="chip chip-estado">{{ diplomaSelecionado.estado }}</span>
          </div>
          <h2>{{ diplomaSelecionado.titulo }}</h2>
          <p v-if="diplomaSelecionado.descricao" class="diploma-desc">{{ diplomaSelecionado.descricao }}</p>
          <p v-if="diplomaSelecionado.dataPublicacao" class="diploma-meta">
            Publicação: {{ String(diplomaSelecionado.dataPublicacao).slice(0, 10) }}
          </p>
        </div>
        <div class="diploma-hero-acoes">
          <BaseButton variant="secondary" @click="abrirEditarDiploma">
            <Pencil :size="14" /> Editar
          </BaseButton>
          <BaseButton variant="danger" @click="eliminarDiplomaActual">
            <Trash2 :size="14" /> Eliminar
          </BaseButton>
        </div>
      </header>

      <div class="artigos-block">
        <div class="artigos-block-head">
          <h3>Artigos</h3>
          <span class="sec-count">{{ diplomaSelecionado.artigos?.length || 0 }}</span>
        </div>
        <div v-if="diplomaSelecionado.artigos?.length" class="lista-artigos">
          <button
            v-for="a in diplomaSelecionado.artigos"
            :key="a.id"
            type="button"
            class="artigo-row"
            @click="abrirArtigo(a.id)"
          >
            <span class="art-num">Art. {{ a.numero }}</span>
            <span class="art-tit">
              {{ a.epigrafe || a.titulo || 'Sem epígrafe' }}
            </span>
            <ChevronRight :size="16" class="art-chevron" />
          </button>
        </div>
        <EmptyState
          v-else
          title="Sem artigos neste diploma"
          description="Importa o PDF completo ou adiciona artigos quando o processamento estiver disponível."
        />
      </div>
    </section>

    <!-- Lista principal -->
    <template v-else>
      <p v-if="erroPesquisa" class="form-erro">{{ erroPesquisa }}</p>

      <section v-if="artigosEncontrados.length || aPesquisarArtigos" class="sec">
        <div class="sec-head">
          <h3>Artigos encontrados</h3>
          <span class="sec-count">{{ artigosEncontrados.length }}</span>
        </div>
        <LoadingState v-if="aPesquisarArtigos" message="A pesquisar artigos…" />
        <div v-else-if="artigosEncontrados.length" class="lista-artigos elev">
          <button
            v-for="a in artigosEncontrados"
            :key="a.id"
            type="button"
            class="artigo-row"
            @click="abrirArtigo(a.id)"
          >
            <span class="art-num">Art. {{ a.numero }}</span>
            <span class="art-tit">
              {{ a.epigrafe || a.titulo || 'Sem epígrafe' }}
              <small v-if="a.diplomaTitulo">{{ a.diplomaTitulo }}</small>
            </span>
            <ChevronRight :size="16" class="art-chevron" />
          </button>
        </div>
        <div v-else-if="pesquisou" class="vazio-painel">
          <h3>Sem artigos para esta pesquisa</h3>
          <p>Experimenta outro número ou palavra-chave, ou abre um diploma na lista abaixo.</p>
        </div>
      </section>

      <section class="sec">
        <div class="sec-head">
          <h3>Diplomas</h3>
          <span class="sec-count">{{ store.diplomas.length }}</span>
        </div>

        <div v-if="store.diplomas.length" class="diploma-grid">
          <button
            v-for="d in store.diplomas"
            :key="d.id"
            type="button"
            class="diploma-card"
            @click="abrirDiploma(d.id)"
          >
            <span class="dc-icon">
              <FeatureArt variant="biblioteca" :size="28" :animated="false" />
            </span>
            <span class="dc-body">
              <span class="dc-titulo">{{ d.titulo }}</span>
              <span class="dc-sub">
                <template v-if="d.numero">{{ d.numero }}</template>
                <template v-if="d.categoriaNome"> · {{ d.categoriaNome }}</template>
                <template v-if="d.dataPublicacao"> · {{ String(d.dataPublicacao).slice(0, 10) }}</template>
              </span>
            </span>
            <span v-if="d.estado" class="chip chip-estado">{{ d.estado }}</span>
            <ChevronRight :size="16" class="art-chevron" />
          </button>
        </div>

        <div v-else-if="semResultados" class="vazio-painel">
          <h3>Nenhum resultado</h3>
          <p>
            Não encontrámos diplomas nem artigos para «{{ termo }}». Experimenta outro termo ou
            importa legislação em <strong>Importar Documentos</strong>.
          </p>
        </div>
        <div v-else-if="!temTermo" class="vazio-painel">
          <FeatureArt variant="biblioteca" :size="40" :animated="false" />
          <h3>Ainda não há diplomas</h3>
          <p>
            Usa <strong>Novo diploma</strong> ou importa um PDF em <strong>Importar Documentos</strong>.
          </p>
        </div>
      </section>
    </template>
  </div>
</template>


<style scoped>
.bib-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 1.25rem 1.1rem 3.5rem;
}

.toolbar {
  display: flex;
  gap: 0.65rem;
  align-items: stretch;
  margin-bottom: 1.25rem;
  padding: 0.75rem;
  border-radius: var(--radius-lg, 16px);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.toolbar-search {
  flex: 1;
  min-width: 0;
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 18px);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  padding: 1.15rem 1.2rem 1.35rem;
  margin-bottom: 1rem;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border);
}

.panel-head h2 {
  margin: 0;
  font-size: 1.05rem;
}

.panel-head p {
  margin: 0.15rem 0 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.field-full {
  grid-column: 1 / -1;
}

.field input,
.field textarea {
  font-family: inherit;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text);
  padding: 0.55rem 0.7rem;
  border-radius: 10px;
  border: 1.5px solid var(--color-border);
  background: var(--color-bg, #faf8f5);
}

.field input:focus,
.field textarea:focus {
  outline: none;
  border-color: var(--color-secondary-400);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-secondary-300) 35%, transparent);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}

.form-erro {
  margin: 0.5rem 0 0;
  color: var(--color-danger);
  font-size: 0.88rem;
  font-weight: 600;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.85rem;
  border: none;
  background: transparent;
  color: var(--color-secondary-700);
  font-family: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.back-link:hover {
  text-decoration: underline;
}

.diploma-hero {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1rem;
  align-items: start;
  margin-bottom: 1.25rem;
  padding: 1rem;
  border-radius: 14px;
  background:
    radial-gradient(circle at 90% 20%, var(--color-primary-100), transparent 50%),
    var(--color-secondary-50, #f4f7ea);
  border: 1px solid var(--color-border);
}

.diploma-hero-art {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.diploma-hero-copy h2 {
  margin: 0.35rem 0 0.25rem;
  font-size: 1.2rem;
  letter-spacing: -0.02em;
  line-height: 1.25;
}

.diploma-desc {
  margin: 0.25rem 0 0;
  font-size: 0.88rem;
  color: var(--color-text-muted);
  line-height: 1.45;
}

.diploma-meta {
  margin: 0.35rem 0 0;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.diploma-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-secondary-700);
}

.chip-muted {
  font-weight: 600;
  color: var(--color-text-muted);
}

.chip-estado {
  background: var(--color-secondary-100);
  border-color: var(--color-secondary-300);
}

.diploma-hero-acoes {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.artigos-block-head {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  margin-bottom: 0.55rem;
}

.artigos-block-head h3 {
  margin: 0;
  font-size: 0.95rem;
}

.sec {
  margin-bottom: 1.35rem;
}

.sec-head {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  margin-bottom: 0.55rem;
}

.sec-head h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
}

.sec-count {
  font-variant-numeric: tabular-nums;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.diploma-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.diploma-card {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  width: 100%;
  text-align: left;
  font-family: inherit;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.diploma-card:hover {
  border-color: var(--color-secondary-300);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.dc-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-secondary-50, #f4f7ea);
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.dc-body {
  flex: 1;
  min-width: 0;
}

.dc-titulo {
  display: block;
  font-size: 0.94rem;
  font-weight: 650;
  color: var(--color-text);
  line-height: 1.3;
}

.dc-sub {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.lista-artigos {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface);
}

.lista-artigos.elev {
  box-shadow: var(--shadow-sm);
}

.artigo-row {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  width: 100%;
  text-align: left;
  font-family: inherit;
  padding: 0.8rem 1rem;
  border: none;
  border-bottom: 1px solid var(--color-border);
  background: transparent;
  cursor: pointer;
  transition: background 0.12s ease;
}

.artigo-row:last-child {
  border-bottom: none;
}

.artigo-row:hover {
  background: var(--color-secondary-50, #f4faf6);
}

.art-num {
  flex-shrink: 0;
  min-width: 4.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-secondary-700);
  padding: 0.25rem 0.45rem;
  border-radius: 8px;
  background: var(--color-secondary-50, #f4f7ea);
  text-align: center;
}

.art-tit {
  flex: 1;
  min-width: 0;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.35;
}

.art-tit small {
  display: block;
  margin-top: 0.15rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-muted);
}

.art-chevron {
  color: var(--color-text-muted);
  opacity: 0.4;
  flex-shrink: 0;
}

.artigo-row:hover .art-chevron,
.diploma-card:hover .art-chevron {
  opacity: 1;
  color: var(--color-secondary-600);
}

.vazio-painel {
  padding: 1.75rem 1.25rem;
  border: 1.5px dashed var(--color-border);
  border-radius: 14px;
  background: var(--color-surface);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
}

.vazio-painel h3 {
  margin: 0.35rem 0 0;
  font-size: 0.98rem;
}

.vazio-painel p {
  margin: 0;
  max-width: 28rem;
  font-size: 0.86rem;
  color: var(--color-text-muted);
  line-height: 1.45;
}

@media (max-width: 640px) {
  .diploma-hero {
    grid-template-columns: 1fr;
  }
  .diploma-hero-acoes {
    flex-direction: row;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .toolbar {
    flex-direction: column;
  }
}
</style>
