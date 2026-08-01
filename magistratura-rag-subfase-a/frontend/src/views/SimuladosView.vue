<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useSimuladoStore } from '@/stores/simulado'
import bibliotecaService from '@/services/bibliotecaService'
import { Timer, Play, CheckCircle, Sparkles, CheckCircle2, AlertTriangle } from 'lucide-vue-next'
import { LoadingState, EmptyState, BaseButton, BaseSelect } from '@/components/ui'
import { PageHero } from '@/components/ui'

const store = useSimuladoStore()
const modo = ref('lista') // lista | em-curso | resultado
const indiceQ = ref(0)
const escolha = ref(null)

// --- Formulário de geração ---
const form = reactive({
  titulo: '',
  descricao: '',
  diplomaId: '',
  artigoId: '',
  assunto: '',
  dificuldade: '',
  quantidadeQuestoes: 5,
  tempoMinutos: 60
})
const diplomas = ref([])
const artigos = ref([])
const aCarregarDiplomas = ref(false)
const aCarregarArtigos = ref(false)
const erroForm = ref('')

function rotuloDiploma(d) {
  if (!d) return ''
  const num = d.numero ? `${d.numero} — ` : ''
  return `${num}${d.titulo || 'Sem título'}`
}

const opcoesDiplomas = computed(() =>
  (diplomas.value || [])
    .map((d) => ({ value: d.id != null ? String(d.id) : '', label: rotuloDiploma(d) }))
    .filter((o) => o.value)
)
const opcoesArtigos = computed(() =>
  (artigos.value || []).map((a) => ({ value: a.id, label: rotuloArtigo(a) }))
)
const opcoesDificuldade = [
  { value: '', label: 'Média' },
  { value: 'FACIL', label: 'Fácil' },
  { value: 'MEDIO', label: 'Média' },
  { value: 'DIFICIL', label: 'Difícil' }
]

function rotuloArtigo(a) {
  if (!a) return ''
  const num = a.numero ? `Art. ${a.numero}` : 'Artigo'
  return a.titulo ? `${num} — ${a.titulo}` : num
}

async function carregarDiplomas() {
  aCarregarDiplomas.value = true
  try {
    const data = await bibliotecaService.listarDiplomas({ size: 500 })
    const lista = Array.isArray(data) ? data : data?.content || []
    diplomas.value = lista.map((d) => ({ ...d, id: d.id != null ? String(d.id) : d.id }))
  } catch (e) {
    console.warn('Falha ao carregar diplomas', e)
    if (!diplomas.value?.length) diplomas.value = []
  } finally {
    aCarregarDiplomas.value = false
  }
}

async function carregarArtigos(diplomaId) {
  if (!diplomaId) {
    artigos.value = []
    return
  }
  aCarregarArtigos.value = true
  try {
    const data = await bibliotecaService.listarArtigos({ diplomaId, size: 300 })
    artigos.value = data.content || data || []
  } catch {
    artigos.value = []
  } finally {
    aCarregarArtigos.value = false
  }
}

watch(
  () => form.diplomaId,
  async (id, oldId) => {
    if (oldId) form.artigoId = ''
    await carregarArtigos(id || undefined)
  }
)

onMounted(async () => {
  await Promise.all([store.carregar(), carregarDiplomas()])
})


const progressoGeracao = reactive({
  activo: false,
  titulo: '',
  etapa: '',
  percentagem: 0,
  segundos: 0
})
let progressoTick = null
const ETAPAS_SIM = [
  { até: 10, etapa: 'A preparar o contexto jurídico…', pct: 15 },
  { até: 25, etapa: 'A gerar questões com IA…', pct: 45 },
  { até: 50, etapa: 'A montar o simulado…', pct: 75 },
  { até: 999, etapa: 'A finalizar e guardar…', pct: 92 }
]
function iniciarProgresso(titulo) {
  if (progressoTick) clearInterval(progressoTick)
  progressoGeracao.activo = true
  progressoGeracao.titulo = titulo
  progressoGeracao.etapa = ETAPAS_SIM[0].etapa
  progressoGeracao.percentagem = 8
  progressoGeracao.segundos = 0
  progressoTick = setInterval(() => {
    progressoGeracao.segundos += 1
    const s = progressoGeracao.segundos
    const etapa = ETAPAS_SIM.find((e) => s <= e.até) || ETAPAS_SIM[ETAPAS_SIM.length - 1]
    progressoGeracao.etapa = etapa.etapa
    if (progressoGeracao.percentagem < etapa.pct) {
      progressoGeracao.percentagem = Math.min(etapa.pct, progressoGeracao.percentagem + 2)
    }
  }, 1000)
}
function pararProgresso(ok = false) {
  if (progressoTick) {
    clearInterval(progressoTick)
    progressoTick = null
  }
  if (ok) {
    progressoGeracao.percentagem = 100
    progressoGeracao.etapa = 'Concluído'
    setTimeout(() => { progressoGeracao.activo = false }, 700)
  } else {
    progressoGeracao.activo = false
  }
}

async function gerarSimulado() {
  erroForm.value = ''
  store.limparFeedbackGeracao()

  const titulo = form.titulo.trim()
  if (!titulo) {
    erroForm.value = 'Indica um título para o simulado.'
    return
  }
  if (!form.diplomaId && !form.artigoId && !form.assunto.trim()) {
    erroForm.value = 'Indica um diploma, um artigo ou um assunto jurídico (pelo menos um).'
    return
  }

  iniciarProgresso(`A gerar simulado «${titulo}»`)
  try {
    await store.gerar({
      titulo,
      descricao: form.descricao.trim() || undefined,
      diplomaId: form.diplomaId || undefined,
      artigoId: form.artigoId || undefined,
      assunto: form.assunto.trim() || undefined,
      dificuldade: form.dificuldade || undefined,
      quantidadeQuestoes: Math.min(Number(form.quantidadeQuestoes) || 5, 15),
      tempoMinutos: Number(form.tempoMinutos) || 60
    })
  } catch {
    // erro já em store.erroGeracao
  } finally {
    pararProgresso(!store.erroGeracao && !!store.sucessoGeracao)
  }
}

async function iniciarAgora() {
  const id = store.ultimoGerado?.simuladoId
  if (!id) return
  store.limparFeedbackGeracao()
  await iniciar(id)
}

async function eliminarSimulado(id, ev) {
  if (ev) ev.stopPropagation()
  if (!confirm('Eliminar este simulado? Esta acção não pode ser anulada.')) return
  try {
    await store.eliminar(id)
  } catch (e) {
    // erro no store
  }
}

async function iniciar(id) {
  await store.iniciar(id)
  modo.value = 'em-curso'
  indiceQ.value = 0
  escolha.value = null
}

async function responderAtual() {
  const q = store.questoes[indiceQ.value]
  if (!q || !escolha.value) return
  await store.responder(q.id, escolha.value)
  escolha.value = null
  if (indiceQ.value < store.questoes.length - 1) {
    indiceQ.value++
  }
}

async function finalizar() {
  await store.finalizar()
  modo.value = 'resultado'
}

function voltarLista() {
  modo.value = 'lista'
  store.resultado = null
  store.carregar()
}
</script>

<template>
  <div class="page">
    <PageHero
      eyebrow="Simulados"
      title="Simulados de Concurso"
      lead="Simula condições reais e mede o teu ritmo."
      art="simulados"
    />

    <!-- LISTA + GERAÇÃO -->
    <div v-if="modo === 'lista'">
      <!-- Progresso geração -->
      <div v-if="progressoGeracao.activo || store.aGerar" class="card progresso-geracao" role="status">
        <div class="progresso-cabecalho">
          <strong>{{ progressoGeracao.titulo || 'A gerar simulado…' }}</strong>
          <span class="progresso-tempo">{{ progressoGeracao.segundos }}s</span>
        </div>
        <p class="progresso-etapa">{{ progressoGeracao.etapa || 'A consultar a IA…' }}</p>
        <div class="progresso-barra-wrap">
          <div class="progresso-barra" :style="{ width: progressoGeracao.percentagem + '%' }"></div>
        </div>
        <p class="progresso-hint">
          Estimativa: {{ progressoGeracao.segundos < 30 ? '30–90 s' : progressoGeracao.segundos < 90 ? '1–3 min' : '2–5 min (modelo local)' }}.
          Não feches a página.
        </p>
      </div>

      <!-- Card de geração -->
      <form class="card form-gerar" @submit.prevent="gerarSimulado">
        <div class="form-gerar-cabecalho">
          <Sparkles :size="20" />
          <div>
            <h2>✨ Criar simulado com IA</h2>
            <p>Prepara uma prova personalizada a partir da biblioteca jurídica real.</p>
          </div>
        </div>

        <div class="field">
          <label>Título *</label>
          <input
            v-model="form.titulo"
            placeholder="Ex.: Constituição — Direitos fundamentais"
            maxlength="250"
            :disabled="store.aGerar"
            required
          />
        </div>

        <div class="field">
          <label>Descrição (opcional)</label>
          <textarea
            v-model="form.descricao"
            rows="2"
            placeholder="Breve descrição da prova…"
            maxlength="2000"
            :disabled="store.aGerar"
          />
        </div>

        <div class="field-row">
          <div class="field">
            <label>Diploma</label>
            <BaseSelect
              v-model="form.diplomaId"
              :options="opcoesDiplomas"
              placeholder="— Nenhum —"
              :disabled="store.aGerar"
              searchable
              search-placeholder="Filtrar diploma…"
            />
            <small v-if="aCarregarDiplomas">A carregar diplomas…</small>
          </div>
          <div class="field">
            <label>Artigo (opcional)</label>
            <BaseSelect
              v-model="form.artigoId"
              :options="opcoesArtigos"
              placeholder="Todo o diploma / nenhum"
              :disabled="store.aGerar || !form.diplomaId"
              searchable
            />
          </div>
        </div>

        <div class="field">
          <label>Assunto (se não escolheres diploma/artigo)</label>
          <input
            v-model="form.assunto"
            placeholder="Ex.: prisão preventiva, presunção de inocência…"
            :disabled="store.aGerar"
          />
        </div>

        <div class="field-row three">
          <div class="field">
            <label>Questões <span class="hint-inline">(recomendado 3–5 em modelos locais)</span></label>
            <input
              v-model.number="form.quantidadeQuestoes"
              type="number"
              min="1"
              max="15"
              :disabled="store.aGerar"
            />
          </div>
          <div class="field">
            <label>Tempo (min)</label>
            <input
              v-model.number="form.tempoMinutos"
              type="number"
              min="5"
              max="300"
              :disabled="store.aGerar"
            />
          </div>
          <div class="field">
            <label>Dificuldade</label>
            <BaseSelect
              v-model="form.dificuldade"
              :options="opcoesDificuldade"
              placeholder="Média"
              :disabled="store.aGerar"
            />
          </div>
        </div>

        <p v-if="erroForm" class="erro">{{ erroForm }}</p>
        <p v-if="store.erroGeracao" class="erro">
          <AlertTriangle :size="14" style="vertical-align: -2px" />
          {{ store.erroGeracao }}
        </p>
        <div v-if="store.sucessoGeracao" class="sucesso-banner">
          <CheckCircle2 :size="18" />
          <span>{{ store.sucessoGeracao }}</span>
          <button type="button" class="btn btn-secondary btn-sm" @click="iniciarAgora">
            <Play :size="14" /> Iniciar agora
          </button>
        </div>

        <BaseButton type="submit" variant="primary" :loading="store.aGerar" loading-text="⏳ A IA está a preparar o teu simulado…">
          <Sparkles :size="16" /> ✨ Gerar simulado
        </BaseButton>
      </form>

      <h3 class="secao-titulo">Simulados disponíveis</h3>

      <LoadingState v-if="store.carregando" message="⏳ A carregar simulados…" />
      <div v-else-if="store.erro" class="center-state"><p class="erro">{{ store.erro }}</p></div>
      <div v-else class="grid grid-2">
        <div v-for="s in store.lista" :key="s.id" class="card simulado-card">
          <Timer :size="20" class="simulado-icon" />
          <h3>{{ s.titulo }}</h3>
          <p class="simulado-meta">
            {{ s.tempoMinutos }} min · {{ s.totalQuestoes }} questões
            <span v-if="s.categoriaNome"> · {{ s.categoriaNome }}</span>
          </p>
          <div class="simulado-acoes">
            <button class="btn btn-primary" @click="iniciar(s.id)">
              <Play :size="15" /> Iniciar
            </button>
            <button type="button" class="btn btn-ghost" title="Eliminar" @click="eliminarSimulado(s.id, $event)">
              Eliminar
            </button>
          </div>
        </div>
        <EmptyState
          v-if="!store.lista.length"
          style="grid-column: 1 / -1"
          title="Ainda não há simulados"
          description="Usa o formulário acima para criar o primeiro com IA, a partir da legislação da biblioteca."
        />
      </div>
    </div>

    <!-- EM CURSO -->
    <div v-else-if="modo === 'em-curso' && store.questoes.length">
      <p class="progresso-q">
        Questão {{ indiceQ + 1 }} / {{ store.questoes.length }}
      </p>
      <div class="card prova-card">
        <p class="enunciado">{{ store.questoes[indiceQ]?.enunciado }}</p>
        <div class="opcoes">
          <button
            v-for="letra in ['A', 'B', 'C', 'D']"
            :key="letra"
            class="btn btn-ghost opcao-btn"
            :class="{ 'btn-secondary': escolha === letra }"
            @click="escolha = letra"
          >
            <strong>{{ letra }})</strong>&nbsp;{{ store.questoes[indiceQ]?.['opcao' + letra] }}
          </button>
        </div>
        <div class="prova-acoes">
          <button class="btn btn-primary" :disabled="!escolha" @click="responderAtual">
            {{ indiceQ < store.questoes.length - 1 ? 'Seguinte' : 'Guardar' }}
          </button>
          <button class="btn btn-secondary" @click="finalizar">Finalizar simulado</button>
        </div>
      </div>
    </div>

    <!-- RESULTADO -->
    <div v-else-if="modo === 'resultado' && store.resultado" class="card resultado-card">
      <CheckCircle :size="36" class="resultado-icon" />
      <h2>Simulado concluído</h2>
      <p class="pontuacao">{{ store.resultado.pontuacao }}%</p>
      <p class="resultado-meta">
        {{ store.resultado.acertos }} corretas · {{ store.resultado.erros }} erradas ·
        {{ store.resultado.totalQuestoes }} total
      </p>
      <button class="btn btn-primary" @click="voltarLista">Voltar aos simulados</button>
    </div>
  </div>
</template>

<style scoped>
.form-gerar {
  max-width: 720px;
  margin: 0 auto 2rem;
  padding: 1.5rem 1.6rem 1.6rem;
  overflow: visible;
}
.form-gerar-cabecalho {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  margin-bottom: 0.25rem;
}
.form-gerar-cabecalho h2 {
  margin: 0;
  font-size: 1.1rem;
}
.form-gerar-cabecalho p {
  margin: 0.2rem 0 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
.form-gerar-cabecalho > svg {
  color: var(--color-secondary-500);
  flex-shrink: 0;
  margin-top: 0.15rem;
}

.field-row {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 1.1rem;
  align-items: start;
}
.field-row.three {
  grid-template-columns: 1fr 1fr 1fr;
}

.erro {
  color: var(--color-danger);
  font-size: 0.85rem;
  margin: 0;
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

.secao-titulo {
  max-width: 960px;
  margin: 0 auto 1rem;
  font-size: 1rem;
}

.simulado-card h3 {
  font-size: 1.05rem;
  margin: 0 0 0.3rem;
}
.simulado-icon {
  color: var(--color-secondary-500);
  margin-bottom: 0.4rem;
}
.simulado-meta {
  font-size: 0.82rem;
  color: var(--color-text-muted);
  margin: 0 0 0.8rem;
}

.empty-state {
  text-align: center;
  padding: 2rem 1.5rem;
}
.empty-state h3 {
  margin: 0 0 0.5rem;
}
.empty-state p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.progresso-q {
  text-align: center;
  color: var(--color-text-muted);
  margin-bottom: 1rem;
}
.prova-card {
  max-width: 640px;
  margin: 0 auto;
}
.enunciado {
  font-weight: 600;
  margin-bottom: 1rem;
}
.opcoes {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.opcao-btn {
  justify-content: flex-start;
  text-align: left;
}
.prova-acoes {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.resultado-card {
  max-width: 480px;
  margin: 0 auto;
  text-align: center;
}
.resultado-icon {
  color: var(--color-accent-500);
  margin-bottom: 0.5rem;
}
.pontuacao {
  font-size: 2rem;
  font-weight: 700;
  margin: 0.5rem 0;
}
.resultado-meta {
  color: var(--color-text-muted);
}

@media (max-width: 720px) {
  .field-row,
  .field-row.three {
    grid-template-columns: 1fr;
  }
  .form-gerar {
    padding: 1.15rem 1.1rem 1.25rem;
  }
}
.simulado-acoes { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }

.progresso-geracao {
  max-width: 720px;
  margin: 0 auto 1.25rem;
  padding: 1.1rem 1.25rem;
}
.progresso-cabecalho {
  display: flex;
  justify-content: space-between;
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
.progresso-hint {
  margin: 0.55rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.hint-inline {
  font-weight: 400;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
</style>
