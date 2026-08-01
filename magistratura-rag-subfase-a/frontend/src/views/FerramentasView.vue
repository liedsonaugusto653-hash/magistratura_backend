<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import tutorService from '@/services/tutorService'
import bibliotecaService from '@/services/bibliotecaService'
import { FileText, BookOpenCheck, Layers, ListChecks, Search, AlertTriangle, RotateCw, CheckCircle2 } from 'lucide-vue-next'
import { BaseSelect } from '@/components/ui'
import { PageHero } from '@/components/ui'

const router = useRouter()

// --- Estado do motor de IA ---
// Todas as ferramentas desta página (resumo, explicação, flashcards, questões) dependem
// do mesmo motor de IA do backend que serve o Tutor. Antes, esta página só descobria uma
// indisponibilidade depois de o utilizador preencher o formulário inteiro e submeter — o
// que parecia um bloqueio arbitrário. Verificamos o estado logo à entrada e mantemos um
// aviso visível e com "tentar novamente", para a limitação ficar clara desde o início.
const iaDisponivel = ref(null)
const iaProviderNome = ref('')
const aVerificarIA = ref(false)

async function verificarEstadoIA() {
  aVerificarIA.value = true
  try {
    const { disponivel, provider } = await tutorService.status()
    iaDisponivel.value = disponivel
    iaProviderNome.value = provider
  } catch {
    iaDisponivel.value = false
  } finally {
    aVerificarIA.value = false
  }
}

const abas = [
  { id: 'resumo', label: 'Resumo', icon: FileText },
  { id: 'explicar', label: 'Explicar Artigo', icon: BookOpenCheck },
  { id: 'flashcards', label: 'Gerar Flashcards', icon: Layers },
  { id: 'questoes', label: 'Gerar Questões', icon: ListChecks }
]
const route = useRoute()
const abaAtiva = ref('resumo')

// --- Biblioteca (dados reais) ---
const diplomas = ref([])
const artigos = ref([])
const aCarregarDiplomas = ref(false)
const aCarregarArtigos = ref(false)
const termoDiploma = ref('')
const termoArtigo = ref('')

async function carregarDiplomas(termo = '') {
  aCarregarDiplomas.value = true
  try {
    const params = { size: 500 }
    const t = (termo || '').trim()
    // Só filtrar no servidor se houver termo real — lista vazia por termo
    // não deve apagar a lista base usada nas outras abas.
    if (t) params.termo = t
    const data = await bibliotecaService.listarDiplomas(params)
    const lista = Array.isArray(data) ? data : data?.content || []
    // Se o filtro devolveu 0 mas não há termo, manter tentativa sem filtro
    if (!lista.length && t) {
      // mantém filtro vazio no servidor — o utilizador vê "sem resultados" no filtro
      diplomas.value = []
    } else {
      diplomas.value = lista.map((d) => ({
        ...d,
        id: d.id != null ? String(d.id) : d.id
      }))
    }
  } catch (e) {
    console.warn('Falha ao carregar diplomas', e)
    // Não limpar se já tínhamos dados (evita apagar a lista por um erro transitório)
    if (!diplomas.value?.length) {
      diplomas.value = []
    }
  } finally {
    aCarregarDiplomas.value = false
  }
}

async function carregarArtigos(diplomaId, termo = '') {
  if (!diplomaId && !termo) {
    artigos.value = []
    return
  }
  aCarregarArtigos.value = true
  try {
    const data = await bibliotecaService.listarArtigos({
      diplomaId: diplomaId || undefined,
      termo: termo || undefined,
      size: 200
    })
    artigos.value = data.content || data || []
  } catch {
    artigos.value = []
  } finally {
    aCarregarArtigos.value = false
  }
}

function rotuloDiploma(d) {
  if (!d) return ''
  const num = d.numero ? `${d.numero} — ` : ''
  return `${num}${d.titulo || 'Sem título'}`
}

function rotuloArtigo(a) {
  if (!a) return ''
  const num = a.numero ? `Art. ${a.numero}` : 'Artigo'
  return a.titulo ? `${num} — ${a.titulo}` : num
}

const opcoesDiplomas = computed(() =>
  (diplomas.value || [])
    .map((d) => ({
      value: d.id != null ? String(d.id) : '',
      label: rotuloDiploma(d)
    }))
    .filter((o) => o.value)
)
const opcoesArtigos = computed(() =>
  (artigos.value || []).map((a) => ({
    value: a.id != null ? String(a.id) : '',
    label: rotuloArtigo(a)
  })).filter((o) => o.value)
)


onMounted(async () => {
  await Promise.all([carregarDiplomas(), verificarEstadoIA()])
  // Prefill a partir da página do artigo
  const q = route.query
  if (q.aba && ['resumo', 'explicar', 'flashcards', 'questoes'].includes(String(q.aba))) {
    abaAtiva.value = String(q.aba)
  }
  if (q.diplomaId) {
    const id = String(q.diplomaId)
    resumoForm.diplomaId = id
    explicarForm.diplomaId = id
    flashForm.diplomaId = id
    questaoForm.diplomaId = id
    await carregarArtigos(id)
  }
  if (q.artigoId) {
    const id = String(q.artigoId)
    resumoForm.artigoId = id
    explicarForm.artigoId = id
    flashForm.artigoId = id
    questaoForm.artigoId = id
  }
})

watch(abaAtiva, (aba) => {
  if (['flashcards', 'questoes', 'resumo', 'explicar'].includes(aba)) {
    garantirDiplomas()
  }
})



// --- Resumo ---
const resumoForm = reactive({ texto: '', diplomaId: '', artigoId: '' })
const resumoResultado = ref('')
const resumoCarregando = ref(false)
const resumoErro = ref('')

watch(
  () => resumoForm.diplomaId,
  async (id) => {
    resumoForm.artigoId = ''
    await carregarArtigos(id || undefined, termoArtigo.value)
  }
)

async function gerarResumo() {
  resumoErro.value = ''
  resumoResultado.value = ''
  if (!resumoForm.texto && !resumoForm.diplomaId && !resumoForm.artigoId) {
    resumoErro.value = 'Escolhe um diploma, um artigo, ou cola um texto a resumir.'
    return
  }
  resumoCarregando.value = true
  try {
    const { resumo } = await tutorService.resumir({
      texto: resumoForm.texto || undefined,
      diplomaId: resumoForm.diplomaId || undefined,
      artigoId: resumoForm.artigoId || undefined
    })
    resumoResultado.value = resumo
  } catch (e) {
    resumoErro.value =
      e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível gerar o resumo.'
  } finally {
    resumoCarregando.value = false
  }
}

// --- Explicar ---
const explicarForm = reactive({ diplomaId: '', artigoId: '', trecho: '' })
const explicarResultado = ref('')
const explicarCarregando = ref(false)
const explicarErro = ref('')

watch(
  () => explicarForm.diplomaId,
  async (id) => {
    explicarForm.artigoId = ''
    await carregarArtigos(id || undefined)
  }
)

async function explicarArtigo() {
  explicarErro.value = ''
  explicarResultado.value = ''
  if (!explicarForm.artigoId) {
    explicarErro.value = 'Escolhe o artigo a explicar.'
    return
  }
  explicarCarregando.value = true
  try {
    const { explicacao } = await tutorService.explicar({
      artigoId: explicarForm.artigoId,
      trecho: explicarForm.trecho || undefined
    })
    explicarResultado.value = explicacao
  } catch (e) {
    explicarErro.value =
      e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível explicar o artigo.'
  } finally {
    explicarCarregando.value = false
  }
}


// --- Progresso visual da geração IA (estilo pipeline de PDFs) ---
const progressoGeracao = reactive({
  activo: false,
  titulo: '',
  etapa: '',
  percentagem: 0,
  segundos: 0
})
let progressoTimer = null
let progressoTick = null

const ETAPAS_GERACAO = [
  { até: 8, etapa: 'A preparar o contexto jurídico…', pct: 15 },
  { até: 20, etapa: 'A consultar o motor de IA…', pct: 40 },
  { até: 45, etapa: 'A IA está a redigir o material…', pct: 70 },
  { até: 90, etapa: 'A validar e guardar resultados…', pct: 90 },
  { até: 999, etapa: 'Ainda a processar — pode demorar com modelos locais…', pct: 95 }
]

function iniciarProgresso(titulo) {
  pararProgresso()
  progressoGeracao.activo = true
  progressoGeracao.titulo = titulo
  progressoGeracao.etapa = ETAPAS_GERACAO[0].etapa
  progressoGeracao.percentagem = 8
  progressoGeracao.segundos = 0
  progressoTick = setInterval(() => {
    progressoGeracao.segundos += 1
    const s = progressoGeracao.segundos
    const etapa = ETAPAS_GERACAO.find((e) => s <= e.até) || ETAPAS_GERACAO[ETAPAS_GERACAO.length - 1]
    progressoGeracao.etapa = etapa.etapa
    // Avanço suave até ao teto da etapa
    const alvo = etapa.pct
    if (progressoGeracao.percentagem < alvo) {
      progressoGeracao.percentagem = Math.min(alvo, progressoGeracao.percentagem + 2)
    }
  }, 1000)
}

function pararProgresso(sucesso = false) {
  if (progressoTick) {
    clearInterval(progressoTick)
    progressoTick = null
  }
  if (sucesso) {
    progressoGeracao.percentagem = 100
    progressoGeracao.etapa = 'Concluído'
  }
  // Pequena pausa visual antes de esconder
  if (progressoGeracao.activo && sucesso) {
    setTimeout(() => {
      progressoGeracao.activo = false
    }, 600)
  } else {
    progressoGeracao.activo = false
  }
}

// --- Flashcards ---
// guardar:true por omissão — sem isto a IA gera, mas nada fica na BD de estudo.
const flashForm = reactive({ diplomaId: '', artigoId: '', quantidade: 3, guardar: true })
const flashResultado = ref([])
const flashCarregando = ref(false)
const flashErro = ref('')
const flashSucesso = ref('')
const flashGuardados = ref(false)
const flashVirados = reactive({})

watch(
  () => flashForm.diplomaId,
  async (id, oldId) => {
    // Não limpar artigo no prefill inicial (oldId vazio) — só quando o utilizador muda de diploma
    if (oldId) {
      flashForm.artigoId = ''
    }
    await carregarArtigos(id || undefined)
  }
)

async function gerarFlashcards() {
  flashErro.value = ''
  flashSucesso.value = ''
  flashGuardados.value = false
  flashResultado.value = []
  Object.keys(flashVirados).forEach((k) => delete flashVirados[k])

  if (!flashForm.diplomaId && !flashForm.artigoId) {
    flashErro.value = 'Escolhe um diploma ou um artigo de origem.'
    return
  }
  if (iaDisponivel.value === false) {
    flashErro.value = 'O motor de IA está indisponível de momento. Tenta novamente mais tarde.'
    return
  }

  flashCarregando.value = true
  iniciarProgresso(`A gerar ${Math.min(Number(flashForm.quantidade) || 3, 5)} flashcards`)
  try {
    const resposta = await tutorService.gerarFlashcards({
      diplomaId: flashForm.diplomaId || undefined,
      artigoId: flashForm.artigoId || undefined,
      quantidade: Math.min(Math.max(Number(flashForm.quantidade) || 3, 1), 5),
      guardar: !!flashForm.guardar
    })
    flashResultado.value = resposta.flashcards || []
    flashGuardados.value = !!resposta.guardados || !!flashForm.guardar

    const n = flashResultado.value.length
    if (n === 0) {
      flashErro.value = 'A IA não devolveu flashcards. Tenta com outro artigo ou quantidade.'
    } else if (flashGuardados.value) {
      flashSucesso.value = `${n} flashcard${n === 1 ? '' : 's'} criado${n === 1 ? '' : 's'} e guardado${n === 1 ? '' : 's'} com sucesso.`
    } else {
      flashSucesso.value = `${n} flashcard${n === 1 ? '' : 's'} gerado${n === 1 ? '' : 's'} (não guardados na biblioteca).`
    }
  } catch (e) {
    flashErro.value =
      e.response?.data?.mensagem ||
      e.response?.data?.message ||
      (e.code === 'ECONNABORTED'
        ? 'O pedido demorou demasiado. Tenta com menos cartões ou verifica o Ollama.'
        : 'Não foi possível gerar flashcards. Verifica se o motor de IA está disponível.')
  } finally {
    flashCarregando.value = false
    pararProgresso(!flashErro.value && flashResultado.value.length > 0)
  }
}

function irParaRevisaoFlashcards() {
  router.push({ name: 'flashcards' })
}

function virar(i) {
  flashVirados[i] = !flashVirados[i]
}

// --- Questões ---
// guardar:true por omissão — sem isto a IA gera, mas nada fica no banco de questões.
const questaoForm = reactive({ diplomaId: '', artigoId: '', quantidade: 2, guardar: true })
const questaoResultado = ref([])
const questaoCarregando = ref(false)
const questaoErro = ref('')
const questaoSucesso = ref('')
const questaoGuardadas = ref(false)
const respostasEscolhidas = reactive({})

watch(
  () => questaoForm.diplomaId,
  async (id, oldId) => {
    if (oldId) {
      questaoForm.artigoId = ''
    }
    await carregarArtigos(id || undefined)
  }
)

async function gerarQuestoes() {
  questaoErro.value = ''
  questaoSucesso.value = ''
  questaoGuardadas.value = false
  questaoResultado.value = []
  Object.keys(respostasEscolhidas).forEach((k) => delete respostasEscolhidas[k])

  if (!questaoForm.diplomaId && !questaoForm.artigoId) {
    questaoErro.value = 'Escolhe um diploma ou um artigo de origem.'
    return
  }
  if (iaDisponivel.value === false) {
    questaoErro.value = 'O motor de IA está indisponível de momento. Tenta novamente mais tarde.'
    return
  }

  questaoCarregando.value = true
  iniciarProgresso(`A gerar ${Math.min(Number(questaoForm.quantidade) || 2, 3)} questões`)
  try {
    const resposta = await tutorService.gerarQuestoes({
      diplomaId: questaoForm.diplomaId || undefined,
      artigoId: questaoForm.artigoId || undefined,
      quantidade: Math.min(Math.max(Number(questaoForm.quantidade) || 2, 1), 3),
      guardar: !!questaoForm.guardar
    })
    questaoResultado.value = resposta.questoes || []
    questaoGuardadas.value = !!resposta.guardados || !!questaoForm.guardar

    const n = questaoResultado.value.length
    if (n === 0) {
      questaoErro.value = 'A IA não devolveu questões. Tenta com outro artigo ou quantidade.'
    } else if (questaoGuardadas.value) {
      questaoSucesso.value = `${n} questão${n === 1 ? '' : 'ões'} criada${n === 1 ? '' : 's'} e guardada${n === 1 ? '' : 's'} com sucesso.`
    } else {
      questaoSucesso.value = `${n} questão${n === 1 ? '' : 'ões'} gerada${n === 1 ? '' : 's'} (não guardadas no banco).`
    }
  } catch (e) {
    questaoErro.value =
      e.response?.data?.mensagem ||
      e.response?.data?.message ||
      (e.code === 'ECONNABORTED'
        ? 'O pedido demorou demasiado. Tenta com menos questões ou verifica o Ollama.'
        : 'Não foi possível gerar questões. Verifica se o motor de IA está disponível.')
  } finally {
    questaoCarregando.value = false
    pararProgresso(!questaoErro.value && questaoResultado.value?.length > 0)
  }
}

function irParaBancoQuestoes() {
  router.push({ name: 'questoes' })
}

function escolher(i, opcao) {
  if (respostasEscolhidas[i]) return
  respostasEscolhidas[i] = opcao
}

async function pesquisarDiplomas() {
  const t = termoDiploma.value.trim()
  await carregarDiplomas(t)
  // Se o utilizador limpou o campo, garantir lista completa
  if (!t && !diplomas.value.length) {
    await carregarDiplomas('')
  }
}

/** Recarrega a lista completa de diplomas (ex.: ao mudar de aba). */
async function garantirDiplomas() {
  if (!diplomas.value.length && !aCarregarDiplomas.value) {
    await carregarDiplomas('')
  }
}
</script>

<template>
  <div class="page">
    <PageHero
      eyebrow="Ferramentas IA"
      title="Cria o teu material de estudo"
      lead="Gera resumos, esquemas e apoio de estudo a partir do que já tens."
      art="tools"
    />
<div v-if="iaDisponivel === false" class="banner-ia">
      <AlertTriangle :size="18" />
      <div class="banner-ia-texto">
        <strong>O motor de IA está indisponível de momento.</strong>
        <span>Resumo, explicação, flashcards e questões usam o mesmo motor do Tutor IA — todos ficam em pausa até ele voltar.</span>
      </div>
      <button type="button" class="btn btn-ghost banner-ia-btn" :disabled="aVerificarIA" @click="verificarEstadoIA">
        <RotateCw :size="14" :class="{ 'a-girar': aVerificarIA }" />
        {{ aVerificarIA ? 'A verificar…' : 'Tentar novamente' }}
      </button>
    </div>

    <div class="tabs">
      <button
        v-for="a in abas"
        :key="a.id"
        class="tab"
        :class="{ ativa: abaAtiva === a.id }"
        @click="abaAtiva = a.id"
      >
        <component :is="a.icon" :size="16" />
        {{ a.label }}
      </button>
    </div>

    
    <!-- Progresso da geração IA -->
    <div v-if="progressoGeracao.activo" class="card progresso-geracao" role="status" aria-live="polite">
      <div class="progresso-cabecalho">
        <strong>{{ progressoGeracao.titulo }}</strong>
        <span class="progresso-tempo">{{ progressoGeracao.segundos }}s</span>
      </div>
      <p class="progresso-etapa">{{ progressoGeracao.etapa }}</p>
      <div class="progresso-barra-wrap">
        <div class="progresso-barra" :style="{ width: progressoGeracao.percentagem + '%' }"></div>
      </div>
      <p class="progresso-hint">
        Estimativa: {{ progressoGeracao.segundos < 30 ? '30–90 s' : progressoGeracao.segundos < 90 ? '1–3 min' : '2–5 min (modelo local)' }}.
        Não feches esta página.
      </p>
    </div>

    <!-- RESUMO -->
    <div v-if="abaAtiva === 'resumo'" class="painel-ferramenta">
      <form class="card form-card" @submit.prevent="gerarResumo">
        <div class="field">
          <label>Texto livre a resumir (opcional)</label>
          <textarea
            v-model="resumoForm.texto"
            rows="5"
            placeholder="Cola aqui um trecho de legislação…"
          ></textarea>
          <small>Em alternativa (ou em complemento), escolhe um diploma ou artigo abaixo.</small>
        </div>

        <div class="field">
          <label>Pesquisar diploma</label>
          <div class="pesquisa-row">
            <input
              v-model="termoDiploma"
              placeholder="Nome ou número do diploma…"
              @keyup.enter="pesquisarDiplomas"
            />
            <button type="button" class="btn btn-ghost" @click="pesquisarDiplomas">
              <Search :size="15" />
            </button>
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label>Diploma (opcional)</label>
            <BaseSelect
              v-model="resumoForm.diplomaId"
              :options="opcoesDiplomas"
              placeholder="Nenhum"
              searchable
              search-placeholder="Filtrar diploma…"
            />
            <small v-if="aCarregarDiplomas">A carregar diplomas…</small>
            <small v-else-if="!(diplomas || []).length">Nenhum diploma na biblioteca.</small>
          </div>
          <div class="field">
            <label>Artigo (opcional)</label>
            <BaseSelect
              v-model="resumoForm.artigoId"
              :options="opcoesArtigos"
              placeholder="Nenhum"
              :disabled="!resumoForm.diplomaId && !(artigos || []).length"
              searchable
              search-placeholder="Filtrar artigo…"
            />
            <small v-if="aCarregarArtigos">A carregar artigos…</small>
          </div>
        </div>

        <p v-if="resumoErro" class="erro">{{ resumoErro }}</p>
        <button class="btn btn-primary" type="submit" :disabled="resumoCarregando">
          {{ resumoCarregando ? 'A gerar resumo…' : 'Gerar resumo' }}
        </button>
      </form>

      <transition name="fade">
        <div v-if="resumoResultado" class="card resultado card-tint-accent">
          <h3>Resumo gerado</h3>
          <p class="resultado-texto">{{ resumoResultado }}</p>
        </div>
      </transition>
    </div>

    <!-- EXPLICAR -->
    <div v-if="abaAtiva === 'explicar'" class="painel-ferramenta">
      <form class="card form-card" @submit.prevent="explicarArtigo">
        <div class="field-row">
          <div class="field">
            <label>Diploma</label>
            <BaseSelect v-model="explicarForm.diplomaId" :options="opcoesDiplomas" placeholder="Escolhe o diploma" searchable />
          </div>
          <div class="field">
            <label>Artigo *</label>
            <BaseSelect v-model="explicarForm.artigoId" :options="opcoesArtigos" placeholder="Escolhe o artigo" searchable />
            <small v-if="explicarForm.diplomaId && !(artigos || []).length && !aCarregarArtigos">
              Este diploma ainda não tem artigos processados.
            </small>
          </div>
        </div>
        <div class="field">
          <label>Trecho específico (opcional)</label>
          <textarea
            v-model="explicarForm.trecho"
            rows="3"
            placeholder="Cola um excerto do artigo, se quiseres focar a explicação"
          ></textarea>
        </div>
        <p v-if="explicarErro" class="erro">{{ explicarErro }}</p>
        <button class="btn btn-primary" type="submit" :disabled="explicarCarregando">
          {{ explicarCarregando ? 'A explicar…' : 'Explicar artigo' }}
        </button>
      </form>

      <transition name="fade">
        <div v-if="explicarResultado" class="card resultado card-tint-secondary">
          <h3>Explicação</h3>
          <p class="resultado-texto">{{ explicarResultado }}</p>
        </div>
      </transition>
    </div>

    <!-- FLASHCARDS -->
    <div v-if="abaAtiva === 'flashcards'" class="painel-ferramenta">
      <form class="card form-card" @submit.prevent="gerarFlashcards">
        <div class="field-row">
          <div class="field">
            <label>Diploma</label>
            <BaseSelect v-model="flashForm.diplomaId" :options="opcoesDiplomas" placeholder="Escolhe o diploma" :disabled="flashCarregando" searchable search-placeholder="Filtrar diploma…" />
            <small v-if="aCarregarDiplomas">A carregar diplomas…</small>
            <small v-else-if="!(opcoesDiplomas || []).length" class="aviso-diploma">
              Nenhum diploma encontrado.
              <button type="button" class="link-btn" @click="carregarDiplomas('')">Recarregar lista</button>
            </small>
            <small v-else>{{ (opcoesDiplomas || []).length }} diploma(s) disponível(eis)</small>
          </div>
          <div class="field">
            <label>Artigo (opcional)</label>
            <BaseSelect v-model="flashForm.artigoId" :options="opcoesArtigos" placeholder="Escolhe o artigo" :disabled="flashCarregando || !flashForm.diplomaId" searchable />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Quantidade <span class="hint-inline">(máx. 5)</span></label>
            <input v-model="flashForm.quantidade" type="number" min="1" max="5" :disabled="flashCarregando" title="Máximo 5 (IA local)" />
          </div>
          <label class="checkbox">
            <input v-model="flashForm.guardar" type="checkbox" :disabled="flashCarregando" />
            Guardar na biblioteca de flashcards
          </label>
        </div>
        <p v-if="flashErro" class="erro">{{ flashErro }}</p>
        <div v-if="flashSucesso" class="sucesso-banner">
          <CheckCircle2 :size="18" />
          <span>{{ flashSucesso }}</span>
          <button
            v-if="flashGuardados"
            type="button"
            class="btn btn-secondary btn-sm"
            @click="irParaRevisaoFlashcards"
          >
            Ir para revisão
          </button>
        </div>
        <button class="btn btn-primary" type="submit" :disabled="flashCarregando || iaDisponivel === false">
          <Layers v-if="!flashCarregando" :size="16" />
          {{ flashCarregando ? '⏳ A IA está a criar os flashcards…' : '✨ Gerar flashcards com IA' }}
        </button>
      </form>

      <div v-if="(flashResultado || []).length" class="grid grid-2 resultado-grid">
        <div
          v-for="(f, i) in flashResultado"
          :key="i"
          class="flashcard"
          :class="{ virado: flashVirados[i] }"
          @click="virar(i)"
        >
          <div class="flashcard-inner">
            <div class="face frente">
              <span class="face-label">Pergunta</span>
              <p>{{ f.pergunta }}</p>
              <small>toca para ver a resposta</small>
            </div>
            <div class="face verso">
              <span class="face-label">Resposta</span>
              <p>{{ f.resposta }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- QUESTÕES -->
    <div v-if="abaAtiva === 'questoes'" class="painel-ferramenta">
      <form class="card form-card" @submit.prevent="gerarQuestoes">
        <div class="field-row">
          <div class="field">
            <label>Diploma</label>
            <BaseSelect v-model="questaoForm.diplomaId" :options="opcoesDiplomas" placeholder="Escolhe o diploma" :disabled="questaoCarregando" searchable search-placeholder="Filtrar diploma…" />
            <small v-if="aCarregarDiplomas">A carregar diplomas…</small>
            <small v-else-if="!(opcoesDiplomas || []).length" class="aviso-diploma">
              Nenhum diploma encontrado.
              <button type="button" class="link-btn" @click="carregarDiplomas('')">Recarregar lista</button>
            </small>
            <small v-else>{{ (opcoesDiplomas || []).length }} diploma(s) disponível(eis)</small>
          </div>
          <div class="field">
            <label>Artigo (opcional)</label>
            <BaseSelect v-model="questaoForm.artigoId" :options="opcoesArtigos" placeholder="Escolhe o artigo" :disabled="questaoCarregando || !questaoForm.diplomaId" searchable />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Quantidade <span class="hint-inline">(máx. 3)</span></label>
            <input v-model="questaoForm.quantidade" type="number" min="1" max="3" :disabled="questaoCarregando" title="Máximo 3 (IA local)" />
          </div>
          <label class="checkbox">
            <input v-model="questaoForm.guardar" type="checkbox" :disabled="questaoCarregando" />
            Guardar no banco de questões
          </label>
        </div>
        <p v-if="questaoErro" class="erro">{{ questaoErro }}</p>
        <div v-if="questaoSucesso" class="sucesso-banner">
          <CheckCircle2 :size="18" />
          <span>{{ questaoSucesso }}</span>
          <button
            v-if="questaoGuardadas"
            type="button"
            class="btn btn-secondary btn-sm"
            @click="irParaBancoQuestoes"
          >
            Ir para o banco de questões
          </button>
        </div>
        <button class="btn btn-primary" type="submit" :disabled="questaoCarregando || iaDisponivel === false">
          <ListChecks v-if="!questaoCarregando" :size="16" />
          {{ questaoCarregando ? '⏳ A IA está a criar as questões…' : '✨ Gerar questões com IA' }}
        </button>
      </form>

      <div v-if="(questaoResultado || []).length" class="quiz">
        <div v-for="(q, i) in questaoResultado" :key="i" class="card questao-card">
          <p class="enunciado">{{ i + 1 }}. {{ q.enunciado }}</p>
          <div class="opcoes">
            <button
              v-for="letra in ['A', 'B', 'C', 'D']"
              :key="letra"
              class="opcao"
              :class="{
                selecionada: respostasEscolhidas[i] === letra,
                correta: respostasEscolhidas[i] && letra === q.respostaCorreta,
                errada: respostasEscolhidas[i] === letra && letra !== q.respostaCorreta
              }"
              @click="escolher(i, letra)"
            >
              <strong>{{ letra }}</strong> {{ q['opcao' + letra] }}
            </button>
          </div>
          <transition name="fade">
            <p v-if="respostasEscolhidas[i]" class="justificacao">
              <strong>Resposta correta: {{ q.respostaCorreta }}.</strong> {{ q.justificacao }}
            </p>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.banner-ia {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  max-width: 720px;
  margin: 0 auto 1.5rem;
  padding: 0.85rem 1.1rem;
  border-radius: var(--radius-md);
  background: #fbf1e2;
  border: 1.5px solid var(--color-warning);
  color: var(--color-text-soft);
}

.banner-ia > svg {
  color: var(--color-warning);
  flex-shrink: 0;
}

.banner-ia-texto {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1;
  font-size: 0.85rem;
}

.banner-ia-texto strong {
  color: var(--color-text);
  font-size: 0.88rem;
}

.banner-ia-btn {
  flex-shrink: 0;
  padding: 0.45rem 0.85rem;
  font-size: 0.8rem;
}

.a-girar {
  animation: girar 0.9s linear infinite;
}

@keyframes girar {
  to {
    transform: rotate(360deg);
  }
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.tab {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  color: var(--color-text-soft);
  padding: 0.55rem 1.1rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.tab.ativa {
  background: var(--color-secondary-500);
  color: #fff;
  border-color: var(--color-secondary-500);
}

.painel-ferramenta {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  align-items: center;
}

.form-card {
  width: 100%;
  max-width: 620px;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.pesquisa-row {
  display: flex;
  gap: 0.5rem;
}
.pesquisa-row input {
  flex: 1;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text-soft);
  align-self: center;
  margin-top: 1.3rem;
  cursor: pointer;
}

.erro {
  color: var(--color-danger);
  font-size: 0.85rem;
}

.sucesso-banner {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-sm);
  background: var(--color-accent-100);
  border: 1px solid var(--color-accent-300);
  color: var(--color-text);
  font-size: 0.88rem;
  font-weight: 500;
}
.sucesso-banner svg {
  color: var(--color-accent-600);
  flex-shrink: 0;
}
.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.78rem;
  margin-left: auto;
}

.resultado {
  width: 100%;
  max-width: 620px;
}

.resultado-texto {
  white-space: pre-wrap;
  color: var(--color-text);
  font-size: 0.92rem;
  line-height: 1.6;
}

.resultado-grid {
  width: 100%;
}

.flashcard {
  perspective: 1200px;
  height: 190px;
  cursor: pointer;
}

.flashcard-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.5s;
  transform-style: preserve-3d;
}

.flashcard.virado .flashcard-inner {
  transform: rotateY(180deg);
}

.face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-shadow: var(--shadow-sm);
}

.frente {
  background: linear-gradient(160deg, var(--color-primary-100), var(--color-surface));
  border: 1px solid var(--color-primary-300);
}

.verso {
  background: linear-gradient(160deg, var(--color-accent-100), var(--color-surface));
  border: 1px solid var(--color-accent-300);
  transform: rotateY(180deg);
}

.face-label {
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
  color: var(--color-secondary-600);
  margin-bottom: 0.4rem;
}

.face p {
  font-size: 0.92rem;
  color: var(--color-text);
  margin: 0 0 0.4rem;
}

.face small {
  color: var(--color-text-muted);
  font-size: 0.72rem;
}

.quiz {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.questao-card {
  text-align: left;
}

.enunciado {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 1rem;
}

.opcoes {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.opcao {
  display: flex;
  gap: 0.6rem;
  text-align: left;
  background: var(--color-surface-alt);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.65rem 0.9rem;
  cursor: pointer;
  font-size: 0.87rem;
  color: var(--color-text-soft);
}

.opcao:hover {
  border-color: var(--color-secondary-300);
}

.opcao.selecionada {
  border-color: var(--color-secondary-500);
}

.opcao.correta {
  background: var(--color-accent-100);
  border-color: var(--color-accent-500);
  color: var(--color-text);
}

.opcao.errada {
  background: #fbeae6;
  border-color: var(--color-danger);
  color: var(--color-text);
}

.justificacao {
  margin-top: 0.9rem;
  font-size: 0.85rem;
  background: var(--color-secondary-100);
  padding: 0.75rem 0.9rem;
  border-radius: var(--radius-sm);
  color: var(--color-secondary-700);
}

@media (max-width: 640px) {
  .field-row {
    grid-template-columns: 1fr;
  }
}

.form-card { overflow: visible; }
.field-row { align-items: start; }
.field { min-width: 0; }

.progresso-geracao {
  max-width: 640px;
  margin: 0 auto 1.25rem;
  padding: 1.1rem 1.25rem;
  border: 1px solid var(--color-secondary-200, #c5d4bc);
  background: var(--color-surface);
}
.progresso-cabecalho {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}
.progresso-tempo {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}
.progresso-etapa {
  margin: 0 0 0.65rem;
  font-size: 0.88rem;
  color: var(--color-text-soft);
}
.progresso-barra-wrap {
  height: 8px;
  border-radius: 999px;
  background: var(--color-primary-100, #e8efe4);
  overflow: hidden;
}
.progresso-barra {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--color-secondary-500, #5a7a4a), var(--color-accent-500, #6b9b6a));
  transition: width 0.6s ease;
}
.link-btn {
  border: none;
  background: none;
  color: var(--color-secondary-600);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  font-size: inherit;
  text-decoration: underline;
}
.aviso-diploma { color: var(--color-danger); }
.progresso-hint {
  margin: 0.55rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
</style>
