<script setup>
/**
 * Artigo — leitura + ações de estudo.
 *
 * Geração contextual directa (Módulo 7):
 * "✨ Gerar flashcards/questões" chama POST /api/ia/* com guardar:true
 * e só depois oferece ir para a lista de revisão.
 * FerramentasView continua como modo avançado (quantidade, etc.).
 */
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import bibliotecaService from '@/services/bibliotecaService'
import favoritoService from '@/services/favoritoService'
import tutorService from '@/services/tutorService'
import { useTutorStore } from '@/stores/tutor'
import ArticleHeader from '@/components/biblioteca/ArticleHeader.vue'
import ArticleMetadata from '@/components/biblioteca/ArticleMetadata.vue'
import ArticleContent from '@/components/biblioteca/ArticleContent.vue'
import ArticleActions from '@/components/biblioteca/ArticleActions.vue'
import ArticleNavigation from '@/components/biblioteca/ArticleNavigation.vue'
import PdfPreview from '@/components/biblioteca/PdfPreview.vue'
import RelatedArticles from '@/components/biblioteca/RelatedArticles.vue'
import { ArrowLeft } from 'lucide-vue-next'
import { BaseButton } from '@/components/ui'
import { PageHero } from '@/components/ui'

const route = useRoute()
const router = useRouter()
const tutor = useTutorStore()

const artigo = ref(null)
const relacionados = ref([])
const carregando = ref(true)
const erro = ref('')
const favorito = ref(false)
const aFavoritar = ref(false)

/** Geração contextual directa */
const gerandoFlashcards = ref(false)
const erroFlashcards = ref('')
const sucessoFlashcards = ref('')
const qtdFlashcards = ref(0)

const gerandoQuestoes = ref(false)
const erroQuestoes = ref('')
const sucessoQuestoes = ref('')
const qtdQuestoes = ref(0)

const QUANTIDADE_PADRAO_FLASHCARDS = 3
const QUANTIDADE_PADRAO_QUESTOES = 2

async function carregar(id) {
  carregando.value = true
  erro.value = ''
  artigo.value = null
  limparFeedbackGeracao()
  try {
    const data = await bibliotecaService.obterArtigo(id)
    artigo.value = data
    await Promise.all([carregarRelacionados(data.diplomaId), carregarEstadoFavorito(id)])
  } catch (e) {
    erro.value =
      e.response?.data?.mensagem || e.response?.data?.message || 'Artigo não encontrado.'
  } finally {
    carregando.value = false
  }
}

function limparFeedbackGeracao() {
  erroFlashcards.value = ''
  sucessoFlashcards.value = ''
  qtdFlashcards.value = 0
  erroQuestoes.value = ''
  sucessoQuestoes.value = ''
  qtdQuestoes.value = 0
}

async function carregarRelacionados(diplomaId) {
  if (!diplomaId) {
    relacionados.value = []
    return
  }
  try {
    const data = await bibliotecaService.listarArtigos({ diplomaId, size: 300 })
    relacionados.value = data.content || data || []
  } catch {
    relacionados.value = []
  }
}

async function carregarEstadoFavorito(artigoId) {
  try {
    const { favorito: f } = await favoritoService.estadoArtigo(artigoId)
    favorito.value = !!f
  } catch {
    favorito.value = false
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (id) carregar(id)
  }
)

onMounted(() => {
  if (route.params.id) carregar(route.params.id)
})

function voltarBiblioteca() {
  if (artigo.value?.diplomaId) {
    router.push({ name: 'biblioteca', query: { diplomaId: artigo.value.diplomaId } })
  } else {
    router.push({ name: 'biblioteca' })
  }
}

function irAnterior() {
  if (artigo.value?.artigoAnteriorId) {
    router.push({ name: 'artigo', params: { id: artigo.value.artigoAnteriorId } })
  }
}
function irSeguinte() {
  if (artigo.value?.artigoSeguinteId) {
    router.push({ name: 'artigo', params: { id: artigo.value.artigoSeguinteId } })
  }
}

function irTutor(mensagem) {
  if (!artigo.value) return
  tutor.definirContexto({
    diplomaId: artigo.value.diplomaId,
    artigoId: artigo.value.id
  })
  sessionStorage.setItem(
    'tutor_prefill',
    JSON.stringify({
      mensagem: mensagem || '',
      diplomaId: artigo.value.diplomaId,
      artigoId: artigo.value.id
    })
  )
  router.push({ name: 'tutor' })
}

function perguntar() {
  irTutor(
    `Explica o Artigo ${artigo.value.numero}${artigo.value.titulo ? ' (' + artigo.value.titulo + ')' : ''} com base no texto oficial.`
  )
}
function explicar() {
  irTutor(
    `Explica o Artigo ${artigo.value.numero} em linguagem simples, adequada a um estudante de magistratura.`
  )
}
function exemplos() {
  irTutor(
    `Dá exemplos práticos de aplicação do Artigo ${artigo.value.numero}, com base apenas no texto oficial.`
  )
}

/**
 * Geração directa: POST /api/ia/flashcards com guardar:true.
 * Não navega para Ferramentas — o utilizador espera geração, não um atalho.
 */
async function gerarFlashcards() {
  if (!artigo.value || gerandoFlashcards.value || gerandoQuestoes.value) return

  erroFlashcards.value = ''
  sucessoFlashcards.value = ''
  qtdFlashcards.value = 0
  gerandoFlashcards.value = true

  try {
    const resposta = await tutorService.gerarFlashcards({
      diplomaId: artigo.value.diplomaId || undefined,
      artigoId: artigo.value.id,
      quantidade: QUANTIDADE_PADRAO_FLASHCARDS,
      guardar: true
    })
    const lista = resposta.flashcards || []
    qtdFlashcards.value = lista.length
    if (lista.length === 0) {
      erroFlashcards.value =
        'A IA não devolveu flashcards. Tenta novamente ou usa Ferramentas IA para ajustar a quantidade.'
    } else {
      sucessoFlashcards.value = `${lista.length} flashcard${lista.length === 1 ? '' : 's'} criado${lista.length === 1 ? '' : 's'} e guardado${lista.length === 1 ? '' : 's'} com sucesso.`
    }
  } catch (e) {
    erroFlashcards.value =
      e.response?.data?.mensagem ||
      e.response?.data?.message ||
      'Não foi possível criar os flashcards. Verifica se a IA está disponível.'
  } finally {
    gerandoFlashcards.value = false
  }
}

/**
 * Geração directa: POST /api/ia/questoes com guardar:true.
 */
async function gerarQuestoes() {
  if (!artigo.value || gerandoFlashcards.value || gerandoQuestoes.value) return

  erroQuestoes.value = ''
  sucessoQuestoes.value = ''
  qtdQuestoes.value = 0
  gerandoQuestoes.value = true

  try {
    const resposta = await tutorService.gerarQuestoes({
      diplomaId: artigo.value.diplomaId || undefined,
      artigoId: artigo.value.id,
      quantidade: QUANTIDADE_PADRAO_QUESTOES,
      guardar: true
    })
    const lista = resposta.questoes || []
    qtdQuestoes.value = lista.length
    if (lista.length === 0) {
      erroQuestoes.value =
        'A IA não devolveu questões. Tenta novamente ou usa Ferramentas IA para ajustar a quantidade.'
    } else {
      sucessoQuestoes.value = `${lista.length} questão${lista.length === 1 ? '' : 'ões'} criada${lista.length === 1 ? '' : 's'} e guardada${lista.length === 1 ? '' : 's'} com sucesso.`
    }
  } catch (e) {
    erroQuestoes.value =
      e.response?.data?.mensagem ||
      e.response?.data?.message ||
      'Não foi possível criar as questões. Verifica se a IA está disponível.'
  } finally {
    gerandoQuestoes.value = false
  }
}

function irRevisaoFlashcards() {
  router.push({ name: 'flashcards' })
}
function irBancoQuestoes() {
  router.push({ name: 'questoes' })
}
/** Atalho para modo avançado (quantidade, outros diplomas) */
function abrirFerramentasAvancado(aba) {
  if (!artigo.value) return
  router.push({
    name: 'ferramentas',
    query: {
      aba,
      diplomaId: artigo.value.diplomaId,
      artigoId: artigo.value.id
    }
  })
}

async function toggleFavorito() {
  if (!artigo.value) return
  aFavoritar.value = true
  try {
    if (favorito.value) {
      await favoritoService.removerArtigo(artigo.value.id)
      favorito.value = false
    } else {
      await favoritoService.adicionarArtigo(artigo.value.id)
      favorito.value = true
    }
  } catch {
    // mantém estado anterior
  } finally {
    aFavoritar.value = false
  }
}
</script>

<template>
  <div class="page artigo-page">
    <button class="btn btn-ghost voltar" type="button" @click="voltarBiblioteca">
      <ArrowLeft :size="16" /> Voltar à biblioteca
    </button>

    <div v-if="carregando" class="center-state">
      <div class="spinner" />
      <p>A carregar artigo…</p>
    </div>
    <div v-else-if="erro" class="center-state">
      <p style="color: var(--color-danger)">{{ erro }}</p>
      <button class="btn btn-primary" type="button" @click="voltarBiblioteca">Voltar</button>
    </div>

    <template v-else-if="artigo">
      <ArticleHeader :artigo="artigo" />
      <ArticleMetadata :artigo="artigo" />

      <div class="layout-duplo">
        <div class="coluna-texto">
          <ArticleContent :texto="artigo.texto" />

          <ArticleActions
            :favorito="favorito"
            :a-favoritar="aFavoritar"
            :gerando-flashcards="gerandoFlashcards"
            :gerando-questoes="gerandoQuestoes"
            @perguntar="perguntar"
            @explicar="explicar"
            @exemplos="exemplos"
            @flashcards="gerarFlashcards"
            @questoes="gerarQuestoes"
            @favorito="toggleFavorito"
          />

          <!-- Feedback geração flashcards -->
          <div v-if="gerandoFlashcards" class="feedback-ia card card-static">
            <div class="spinner-sm" />
            <p>A IA está a criar flashcards a partir deste artigo…</p>
          </div>
          <div v-else-if="sucessoFlashcards" class="sucesso-banner">
            <span>✓ {{ sucessoFlashcards }}</span>
            <BaseButton variant="secondary" class="btn-sm-inline" @click="irRevisaoFlashcards">
              Ir para revisão
            </BaseButton>
          </div>
          <div v-else-if="erroFlashcards" class="erro-banner">
            <span>{{ erroFlashcards }}</span>
            <button type="button" class="btn btn-ghost btn-sm-inline" @click="gerarFlashcards">
              Tentar novamente
            </button>
            <button
              type="button"
              class="btn btn-ghost btn-sm-inline"
              @click="abrirFerramentasAvancado('flashcards')"
            >
              Modo avançado
            </button>
          </div>

          <!-- Feedback geração questões -->
          <div v-if="gerandoQuestoes" class="feedback-ia card card-static">
            <div class="spinner-sm" />
            <p>A IA está a criar questões a partir deste artigo…</p>
          </div>
          <div v-else-if="sucessoQuestoes" class="sucesso-banner">
            <span>✓ {{ sucessoQuestoes }}</span>
            <BaseButton variant="secondary" class="btn-sm-inline" @click="irBancoQuestoes">
              Ir para o banco de questões
            </BaseButton>
          </div>
          <div v-else-if="erroQuestoes" class="erro-banner">
            <span>{{ erroQuestoes }}</span>
            <button type="button" class="btn btn-ghost btn-sm-inline" @click="gerarQuestoes">
              Tentar novamente
            </button>
            <button
              type="button"
              class="btn btn-ghost btn-sm-inline"
              @click="abrirFerramentasAvancado('questoes')"
            >
              Modo avançado
            </button>
          </div>

          <ArticleNavigation
            :anterior-id="artigo.artigoAnteriorId"
            :seguinte-id="artigo.artigoSeguinteId"
            @anterior="irAnterior"
            @seguinte="irSeguinte"
          />
          <RelatedArticles
            :artigos="relacionados"
            :atual-id="artigo.id"
            :capitulo="artigo.capitulo"
          />
        </div>
        <div class="coluna-pdf">
          <PdfPreview
            :documento-id="artigo.documentoId"
            :pagina="artigo.paginaInicio"
            :pagina-fim="artigo.paginaFim"
            :rotulo="artigo.numero ? 'Art. ' + artigo.numero : ''"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.artigo-page {
  max-width: 1280px;
}
.voltar {
  margin-bottom: 1rem;
}
.layout-duplo {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 1.25rem;
  align-items: start;
}
.coluna-texto,
.coluna-pdf {
  min-width: 0;
}
@media (max-width: 1100px) {
  .layout-duplo {
    grid-template-columns: 1fr 0.9fr;
    gap: 1rem;
  }
}
@media (max-width: 960px) {
  .layout-duplo {
    grid-template-columns: 1fr;
  }
  .coluna-pdf {
    order: 2;
  }
  .coluna-texto {
    order: 1;
  }
}
@media (max-width: 560px) {
  .artigo-page {
    max-width: 100%;
  }
  .voltar {
    margin-bottom: 0.75rem;
  }
}
.feedback-ia {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0.85rem 0;
  padding: 0.85rem 1rem;
  font-size: 0.9rem;
  color: var(--color-text-soft);
}
.feedback-ia p {
  margin: 0;
}
.spinner-sm {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-secondary-500);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.erro-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin: 0.85rem 0;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-sm);
  background: #fbeae6;
  border: 1px solid #e8b4a8;
  color: var(--color-danger);
  font-size: 0.88rem;
}
.btn-sm-inline {
  padding: 0.3rem 0.75rem !important;
  font-size: 0.78rem !important;
}
</style>
