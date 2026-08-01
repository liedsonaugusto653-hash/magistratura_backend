<script setup>
import PageHint from '@/components/ui/PageHint.vue'
import { confirmarEliminacao } from '@/utils/prefsUi'
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { UploadCloud, FileText, PlayCircle, ExternalLink, Plus, Trash2 } from 'lucide-vue-next'
import { BaseSelect } from '@/components/ui'
import documentoService from '@/services/documentoService'
import { subscreverProgressoDocumento } from '@/services/documentoProgressSse'
import bibliotecaService from '@/services/bibliotecaService'
import { PageHero } from '@/components/ui'

// --- Upload ---
const form = reactive({
  ficheiro: null,
  titulo: '',
  categoriaId: '',
  fonte: '',
  oficial: true,
  dataPublicacao: ''
})
const nomeFicheiro = ref('')
const aEnviar = ref(false)
const erroUpload = ref('')
const sucessoUpload = ref('')
const categorias = ref([])

function selecionarFicheiro(e) {
  const f = e.target.files[0]
  if (!f) return
  if (f.type !== 'application/pdf') {
    erroUpload.value = 'Apenas ficheiros PDF são aceites.'
    e.target.value = ''
    form.ficheiro = null
    nomeFicheiro.value = ''
    return
  }
  erroUpload.value = ''
  form.ficheiro = f
  nomeFicheiro.value = f.name
  if (!form.titulo) {
    form.titulo = f.name.replace(/\.pdf$/i, '')
  }
}

async function enviarDocumento() {
  erroUpload.value = ''
  sucessoUpload.value = ''
  if (!form.ficheiro) {
    erroUpload.value = 'Escolhe um ficheiro PDF para importar.'
    return
  }
  if (!form.titulo) {
    erroUpload.value = 'Indica um título para o documento.'
    return
  }
  aEnviar.value = true
  try {
    const doc = await documentoService.importar({
      ficheiro: form.ficheiro,
      titulo: form.titulo,
      categoriaId: form.categoriaId || undefined,
      fonte: form.fonte || undefined,
      oficial: form.oficial,
      dataPublicacao: form.dataPublicacao || undefined
    })
    sucessoUpload.value = doc.tipoPdf === 'PROTECTED'
      ? `Documento "${doc.titulo}" importado — atenção: PDF com protecção. Exporte sem restrições antes de processar.`
      : `Documento "${doc.titulo}" importado com sucesso.`
    form.ficheiro = null
    form.titulo = ''
    form.categoriaId = ''
    form.fonte = ''
    form.dataPublicacao = ''
    nomeFicheiro.value = ''
    await carregarDocumentos()
  } catch (e) {
    erroUpload.value = e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível importar o documento.'
  } finally {
    aEnviar.value = false
  }
}

// --- Listagem ---
const documentos = ref([])
const aCarregar = ref(false)

async function carregarDocumentos() {
  aCarregar.value = true
  try {
    const data = await documentoService.listar()
    documentos.value = data.content || data
  } finally {
    aCarregar.value = false
  }
}

// --- Processar (estruturar artigos) ---
const diplomas = ref([])
const processandoId = ref(null)
const processamentoAtivo = reactive({})
const progressoUi = reactive({}) // { [id]: { mensagem, percentagem, segundos, estimado } }
const progressoTimers = {} // { [id]: intervalId }

function iniciarCronometroProgresso(docId) {
  pararCronometroProgresso(docId)
  if (!progressoUi[docId]) {
    progressoUi[docId] = { mensagem: 'A processar…', percentagem: 0, segundos: 0, estimado: '1–5 min' }
  } else {
    progressoUi[docId].segundos = 0
    progressoUi[docId].estimado = progressoUi[docId].estimado || '1–5 min'
  }
  progressoTimers[docId] = setInterval(() => {
    if (!progressoUi[docId]) return
    progressoUi[docId].segundos = (progressoUi[docId].segundos || 0) + 1
    const s = progressoUi[docId].segundos
    // Estimativa dinâmica grosseira
    if (s < 30) progressoUi[docId].estimado = '1–3 min'
    else if (s < 90) progressoUi[docId].estimado = '2–5 min'
    else if (s < 180) progressoUi[docId].estimado = '3–8 min'
    else progressoUi[docId].estimado = 'pode demorar mais — OCR em PDFs grandes'
  }, 1000)
}

function pararCronometroProgresso(docId) {
  if (progressoTimers[docId]) {
    clearInterval(progressoTimers[docId])
    delete progressoTimers[docId]
  }
}

const cancelarSse = reactive({})
const mostrarCriarDiploma = ref(false)
const aCriarDiploma = ref(false)
const erroCriarDiploma = ref('')
const novoDiploma = reactive({
  numero: '',
  titulo: '',
  descricao: '',
  dataPublicacao: '',
  categoriaId: '',
  docIdParaAssociar: null
})

const diplomaEscolhido = reactive({})

const opcoesCategorias = computed(() =>
  (categorias.value || []).map((c) => ({ value: c.id, label: c.nome }))
)
const opcoesDiplomas = computed(() =>
  (diplomas.value || []).map((d) => ({
    value: d.id,
    label: d.numero ? `${d.numero} — ${d.titulo || ''}` : (d.titulo || 'Sem título')
  }))
)

const erroProcessar = reactive({})
const avisoProcessar = reactive({})

// --- Visualizar PDF (autenticado) ---
const aAbrirPdf = reactive({})

async function verPdf(doc) {
  aAbrirPdf[doc.id] = true
  erroProcessar[doc.id] = ''
  try {
    const blob = await documentoService.obterPdfBlob(doc.id)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    // liberta a memória um pouco depois de a aba ter tido tempo de carregar o ficheiro
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (e) {
    erroProcessar[doc.id] = 'Não foi possível abrir o PDF.'
  } finally {
    aAbrirPdf[doc.id] = false
  }
}


async function processarDocumento(doc) {
  const diplomaId = diplomaEscolhido[doc.id]
  if (!diplomaId) {
    if (!diplomas.value.length) {
      erroProcessar[doc.id] =
        'Ainda não tens diplomas. Clica em «Novo diploma» para criar um e depois processa o PDF.'
    } else {
      erroProcessar[doc.id] = 'Escolhe o diploma ao qual este PDF pertence.'
    }
    return
  }
  if (processamentoAtivo[doc.id]) return

  erroProcessar[doc.id] = ''
  avisoProcessar[doc.id] = ''
  processandoId.value = doc.id
  processamentoAtivo[doc.id] = true
  progressoUi[doc.id] = { mensagem: 'A ligar ao servidor…', percentagem: 0, segundos: 0, estimado: '1–5 min' }
  iniciarCronometroProgresso(doc.id)

  // Cancela SSE anterior se existir
  cancelarSse[doc.id]?.()

  const terminar = async (finalEstado) => {
    pararCronometroProgresso(doc.id)
    cancelarSse[doc.id]?.()
    delete cancelarSse[doc.id]
    processamentoAtivo[doc.id] = false
    processandoId.value = null

    if (finalEstado === 'PROCESSADO' || finalEstado === 'PROCESSADO_COM_AVISOS') {
      progressoUi[doc.id] = {
        mensagem: progressoUi[doc.id]?.mensagem || 'Processamento concluído',
        percentagem: 100
      }
    }

    await carregarDocumentos()

    if (finalEstado === 'ERRO' || finalEstado === 'FALHA_EXTRACAO') {
      erroProcessar[doc.id] =
        progressoUi[doc.id]?.mensagem || 'Processamento falhou.'
    } else {
      // Limpa barra após refresh bem-sucedido (evita UI stale)
      setTimeout(() => {
        if (!processamentoAtivo[doc.id]) delete progressoUi[doc.id]
      }, 2500)
    }
  }

  // Abre SSE ANTES do POST para não perder eventos iniciais
  cancelarSse[doc.id] = subscreverProgressoDocumento(doc.id, {
    onProgress(p) {
      progressoUi[doc.id] = {
        mensagem: p.mensagem || 'A processar…',
        percentagem: p.percentagem ?? progressoUi[doc.id]?.percentagem ?? 0
      }
      // Actualiza estado local no cartão sem tempestade de pedidos
      const d = documentos.value.find((x) => x.id === doc.id)
      if (d) {
        d.estado = p.estado || d.estado
        d.mensagemProgresso = p.mensagem
        d.progressoPercentagem = p.percentagem
      }
    },
    onDone(p) {
      progressoUi[doc.id] = {
        mensagem: p.mensagem || 'Concluído',
        percentagem: 100
      }
      terminar(p.estado || 'PROCESSADO')
    },
    onError(p) {
      progressoUi[doc.id] = {
        mensagem: p.mensagem || 'Erro',
        percentagem: p.percentagem ?? 0
      }
      terminar('ERRO')
    }
  })

  try {
    const iniciado = await documentoService.processar(doc.id, diplomaId)
    // Actualiza só o cartão local (sem GET lista)
    const d = documentos.value.find((x) => x.id === doc.id)
    if (d && iniciado) {
      d.estado = iniciado.estado || 'PROCESSANDO'
      if (iniciado.mensagemProgresso) d.mensagemProgresso = iniciado.mensagemProgresso
      if (iniciado.progressoPercentagem != null) d.progressoPercentagem = iniciado.progressoPercentagem
    }
    // Não refrescar a lista aqui — o cartão já está PROCESSANDO via SSE.
    // Um único GET /documentos no terminar() (done/error).
    // Fallback: se SSE não enviar done em 15 min, libertar UI
    setTimeout(() => {
      if (processamentoAtivo[doc.id]) {
        avisoProcessar[doc.id] =
          'O processamento continua no servidor. Actualiza a página para ver o resultado final.'
        processamentoAtivo[doc.id] = false
        processandoId.value = null
        cancelarSse[doc.id]?.()
      }
    }, 15 * 60 * 1000)
  } catch (e) {
    // Job pode ter sido agendado mesmo com erro de rede — SSE continua a ouvir
    const msg =
      e.response?.data?.mensagem || e.response?.data?.message || e.message || ''
    if (msg) avisoProcessar[doc.id] = msg
    // Se for erro 4xx real (diploma em falta), fecha SSE
    const status = e.response?.status
    if (status && status >= 400 && status < 500) {
      await terminar('ERRO')
      erroProcessar[doc.id] = msg || 'Não foi possível iniciar o processamento.'
    }
  }
}


// --- Eliminar ---
const eliminandoId = ref(null)

async function eliminarDocumento(doc) {
  const confirmado = confirmarEliminacao(
    `Eliminar "${doc.titulo}"? Esta ação remove o PDF, os artigos extraídos e os embeddings associados. Não pode ser desfeita.`
  )
  if (!confirmado) return

  erroProcessar[doc.id] = ''
  eliminandoId.value = doc.id
  try {
    await documentoService.eliminar(doc.id, false)
    documentos.value = documentos.value.filter((d) => d.id !== doc.id)
  } catch (e) {
    const msg =
      e.response?.data?.mensagem ||
      e.response?.data?.message ||
      'Não foi possível eliminar o documento.'
    // Documento preso em processamento → oferecer forçar
    if (/processado|processamento|aguarde/i.test(msg)) {
      const forcar = confirmarEliminacao(
        msg + '\n\nO documento parece preso. Queres forçar a eliminação?'
      )
      if (forcar) {
        try {
          await documentoService.eliminar(doc.id, true)
          documentos.value = documentos.value.filter((d) => d.id !== doc.id)
          return
        } catch (e2) {
          erroProcessar[doc.id] =
            e2.response?.data?.mensagem ||
            e2.response?.data?.message ||
            'Falha ao forçar eliminação.'
          return
        }
      }
    }
    erroProcessar[doc.id] = msg
  } finally {
    eliminandoId.value = null
  }
}


async function carregarDiplomas() {
  const d = await bibliotecaService.listarDiplomas({ size: 200 })
  diplomas.value = d.content || d || []
}

async function abrirCriarDiploma(docId = null) {
  erroCriarDiploma.value = ''
  novoDiploma.numero = ''
  novoDiploma.titulo = form.titulo || ''
  novoDiploma.descricao = ''
  novoDiploma.dataPublicacao = form.dataPublicacao || ''
  novoDiploma.categoriaId = form.categoriaId || ''
  novoDiploma.docIdParaAssociar = docId
  mostrarCriarDiploma.value = true
}

async function criarDiplomaEContinuar() {
  erroCriarDiploma.value = ''
  if (!novoDiploma.numero.trim() || !novoDiploma.titulo.trim()) {
    erroCriarDiploma.value = 'Número e título do diploma são obrigatórios.'
    return
  }
  aCriarDiploma.value = true
  try {
    const criado = await bibliotecaService.criarDiploma({
      numero: novoDiploma.numero.trim(),
      titulo: novoDiploma.titulo.trim(),
      descricao: novoDiploma.descricao || undefined,
      dataPublicacao: novoDiploma.dataPublicacao || undefined,
      categoriaId: novoDiploma.categoriaId || undefined
    })
    await carregarDiplomas()
    if (novoDiploma.docIdParaAssociar) {
      diplomaEscolhido[novoDiploma.docIdParaAssociar] = criado.id
    }
    mostrarCriarDiploma.value = false
  } catch (e) {
    erroCriarDiploma.value =
      e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível criar o diploma.'
  } finally {
    aCriarDiploma.value = false
  }
}

function corEstado(estado) {
  switch (estado) {
    case 'PROCESSADO':
    case 'PROCESSADO_COM_AVISOS':
      return 'estado-ok'
    case 'ERRO':
    case 'FALHA_EXTRACAO':
      return 'estado-erro'
    case 'PROCESSANDO':
    case 'ANALISANDO':
    case 'EXTRAINDO_TEXTO':
    case 'OCR_EM_EXECUCAO':
    case 'ESTRUTURANDO':
      return 'estado-processando'
    case 'IMPORTADO':
      return 'estado-neutro'
    default:
      return 'estado-neutro'
  }
}

function rotuloEstado(doc) {
  return doc.estadoRotulo || doc.estado || '—'
}

function isPdfProtegido(doc) {
  if (doc.codigoErro && String(doc.codigoErro).startsWith('PDF_PROTECTED')) return true
  if (doc.tipoPdf === 'PROTECTED') return true
  const t = `${doc.mensagemProgresso || ''} ${doc.observacoesProcessamento || ''} ${doc.resumoResultado || ''}`.toLowerCase()
  return t.includes('protegido') || t.includes('restriç') || t.includes('imprimir para pdf')
}

function mensagemErroDoc(doc) {
  return (
    erroProcessar[doc.id]
    || doc.mensagemProgresso
    || doc.observacoesProcessamento
    || doc.resumoResultado
    || 'Não foi possível processar este documento.'
  )
}

const ACOES_PDF_PROTEGIDO = [
  'Abra o PDF no Chrome, Edge ou Adobe Reader',
  'Imprimir → «Guardar como PDF» / «Microsoft Print to PDF»',
  'Elimine este documento e importe a versão sem protecção',
  'Associe o diploma e volte a processar'
]

function acoesPara(doc) {
  if (doc.acoesSugeridas?.length) return doc.acoesSugeridas
  if (isPdfProtegido(doc) || isPdfProtegido({ mensagemProgresso: erroProcessar[doc.id] })) {
    return ACOES_PDF_PROTEGIDO
  }
  return []
}

onUnmounted(() => {
  Object.values(cancelarSse).forEach((fn) => {
    try { fn?.() } catch { /* ignore */ }
  })
})

onMounted(async () => {
  await Promise.all([
    carregarDocumentos(),
    bibliotecaService.listarCategorias().then((c) => (categorias.value = c)),
    carregarDiplomas()
  ])
})
</script>

<template>
  <div class="page">
    <PageHero
      eyebrow="Biblioteca"
      title="Importar Documentos"
      lead="Carrega diplomas e textos para a biblioteca jurídica."
      art="biblioteca"
    />
<!-- FORMULÁRIO DE UPLOAD -->
    <form class="card form-card" @submit.prevent="enviarDocumento">
      <div class="field">
        <label>Ficheiro PDF *</label>
        <label class="dropzone" :class="{ 'tem-ficheiro': nomeFicheiro }">
          <UploadCloud :size="22" />
          <span>{{ nomeFicheiro || 'Clica para escolher um ficheiro PDF' }}</span>
          <input type="file" accept="application/pdf" @change="selecionarFicheiro" hidden />
        </label>
      </div>

      <div class="field">
        <label>Título *</label>
        <input v-model="form.titulo" placeholder="Ex.: Código Penal Angolano" required />
      </div>

      <div class="field-row">
        <div class="field">
          <label>Categoria (opcional)</label>
          <BaseSelect
            v-model="form.categoriaId"
            :options="opcoesCategorias"
            placeholder="Sem categoria"
            searchable
            search-placeholder="Filtrar categoria…"
          />
        </div>
        <div class="field">
          <label>Fonte (opcional)</label>
          <input v-model="form.fonte" placeholder="Ex.: Diário da República" />
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label>Data de publicação (opcional)</label>
          <input v-model="form.dataPublicacao" type="date" />
        </div>
        <label class="checkbox">
          <input v-model="form.oficial" type="checkbox" />
          Fonte oficial
        </label>
      </div>

      <p v-if="erroUpload" class="erro">{{ erroUpload }}</p>
      <p v-if="sucessoUpload" class="sucesso">{{ sucessoUpload }}</p>

      <button class="btn btn-primary" type="submit" :disabled="aEnviar">
        {{ aEnviar ? 'A importar…' : 'Importar PDF' }}
      </button>
    </form>

    <!-- LISTA DE DOCUMENTOS -->
    <h3 class="secao-titulo">Documentos importados</h3>

    <div v-if="aCarregar" class="center-state"><div class="spinner" /><p>A carregar…</p></div>

    <div v-else class="lista-documentos">
      <div v-for="doc in documentos" :key="doc.id" class="card documento-card">
        <div class="documento-cabecalho">
          <FileText :size="20" style="color: var(--color-secondary-500)" />
          <div class="documento-info">
            <strong>{{ doc.titulo }}</strong>
            <span class="documento-meta">
              {{ doc.numeroPaginas }} páginas · importado em {{ doc.dataImportacao?.slice(0, 10) }}
            </span>
          </div>
          <span class="estado-badge" :class="corEstado(doc.estado)">{{ rotuloEstado(doc) }}</span>
        </div>

        <div
          v-if="doc.tipoPdf === 'PROTECTED' && doc.estado === 'IMPORTADO'"
          class="alerta-protegido"
        >
          <strong>PDF com protecção detectada</strong>
          <p>{{ doc.mensagemProgresso || doc.observacoesProcessamento }}</p>
          <ol class="acoes-lista">
            <li v-for="(a, i) in ACOES_PDF_PROTEGIDO" :key="i">{{ a }}</li>
          </ol>
        </div>

        <p
          v-else-if="doc.observacoesProcessamento && doc.estado !== 'FALHA_EXTRACAO' && doc.estado !== 'ERRO'"
          class="documento-obs"
        >{{ doc.observacoesProcessamento }}</p>

        <div
          v-if="progressoUi[doc.id] || ['PROCESSANDO','ANALISANDO','EXTRAINDO_TEXTO','OCR_EM_EXECUCAO','ESTRUTURANDO'].includes(doc.estado)"
          class="documento-obs progresso"
        >
          <div class="progresso-cabecalho">
            <span class="progresso-texto">
              {{
                progressoUi[doc.id]?.mensagem
                  || doc.mensagemProgresso
                  || 'A processar…'
              }}
            </span>
            <span class="progresso-meta">
              <template v-if="(progressoUi[doc.id]?.percentagem ?? doc.progressoPercentagem) != null">
                {{ progressoUi[doc.id]?.percentagem ?? doc.progressoPercentagem }}%
              </template>
              <template v-if="progressoUi[doc.id]?.segundos != null">
                · {{ progressoUi[doc.id].segundos }}s
              </template>
            </span>
          </div>
          <div class="progresso-bar">
            <div
              class="progresso-fill"
              :style="{
                width:
                  (progressoUi[doc.id]?.percentagem
                    ?? doc.progressoPercentagem
                    ?? 0) + '%'
              }"
            />
          </div>
          <p v-if="progressoUi[doc.id]?.estimado" class="progresso-hint">
            Estimativa: {{ progressoUi[doc.id].estimado }}. Não feches esta página.
          </p>
        </div>

        <div class="documento-acoes">
          <button class="btn btn-ghost" :disabled="aAbrirPdf[doc.id]" @click="verPdf(doc)">
            <ExternalLink :size="15" /> {{ aAbrirPdf[doc.id] ? 'A abrir…' : 'Ver PDF' }}
          </button>

          <button
            class="btn btn-danger"
            :disabled="eliminandoId === doc.id"
            @click="eliminarDocumento(doc)"
          >
            <Trash2 :size="15" /> {{ eliminandoId === doc.id ? 'A eliminar…' : 'Eliminar' }}
          </button>

          <template v-if="doc.estado !== 'PROCESSADO'">
            <BaseSelect
              v-model="diplomaEscolhido[doc.id]"
              class="select-diploma"
              :options="opcoesDiplomas"
              placeholder="Escolhe o diploma…"
              searchable
              search-placeholder="Filtrar diploma…"
            />
            <button
              type="button"
              class="btn btn-ghost"
              title="Criar novo diploma"
              @click="abrirCriarDiploma(doc.id)"
            >
              <Plus :size="15" /> Novo diploma
            </button>
            <button
              class="btn btn-secondary"
              :disabled="processandoId === doc.id || processamentoAtivo[doc.id]"
              @click="processarDocumento(doc)"
            >
              <PlayCircle :size="15" />
              {{ (processandoId === doc.id || processamentoAtivo[doc.id]) ? 'A processar…' : 'Processar' }}
            </button>
          </template>
        </div>
        <div
          v-if="erroProcessar[doc.id] || doc.estado === 'FALHA_EXTRACAO' || doc.estado === 'ERRO'"
          class="alerta-falha"
        >
          <strong>{{ isPdfProtegido(doc) || isPdfProtegido({ mensagemProgresso: erroProcessar[doc.id] }) ? 'Não foi possível extrair o texto' : 'Processamento interrompido' }}</strong>
          <p>{{ mensagemErroDoc(doc) }}</p>
          <ol v-if="acoesPara(doc).length" class="acoes-lista">
            <li v-for="(a, i) in acoesPara(doc)" :key="i">{{ a }}</li>
          </ol>
          <p class="alerta-hint">
            Dica: «Imprimir para PDF» remove a maioria das restrições dos diplomas oficiais sem alterar o conteúdo.
          </p>
        </div>
        <p v-if="avisoProcessar[doc.id]" class="aviso">{{ avisoProcessar[doc.id] }}</p>
      </div>

      <p v-if="!documentos.length" class="center-state">Ainda não importaste nenhum documento.</p>
    </div>

    <!-- Modal criar diploma (não interrompe o fluxo de upload/processamento) -->
    <div v-if="mostrarCriarDiploma" class="modal-backdrop" @click.self="mostrarCriarDiploma = false">
      <div class="modal card">
        <h3>Criar diploma</h3>
        <p class="modal-hint">
          Cria o diploma para associares o PDF. Os dados do formulário de upload não são perdidos.
        </p>
        <div class="field">
          <label>Número *</label>
          <input v-model="novoDiploma.numero" placeholder="Ex.: Lei n.º 1/10" />
        </div>
        <div class="field">
          <label>Título *</label>
          <input v-model="novoDiploma.titulo" placeholder="Ex.: Constituição da República de Angola" />
        </div>
        <div class="field">
          <label>Descrição (opcional)</label>
          <textarea v-model="novoDiploma.descricao" rows="2" />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Categoria</label>
            <BaseSelect
              v-model="novoDiploma.categoriaId"
              :options="opcoesCategorias"
              placeholder="Sem categoria"
              searchable
            />
          </div>
          <div class="field">
            <label>Data de publicação</label>
            <input v-model="novoDiploma.dataPublicacao" type="date" />
          </div>
        </div>
        <p v-if="erroCriarDiploma" class="erro">{{ erroCriarDiploma }}</p>
        <div class="modal-acoes">
          <button type="button" class="btn btn-ghost" @click="mostrarCriarDiploma = false">Cancelar</button>
          <button type="button" class="btn btn-primary" :disabled="aCriarDiploma" @click="criarDiplomaEContinuar">
            {{ aCriarDiploma ? 'A criar…' : 'Criar e continuar' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.form-card {
  max-width: 640px;
  margin: 0 auto 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
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

.dropzone {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-sm);
  padding: 1rem 1.1rem;
  cursor: pointer;
  color: var(--color-text-muted);
  background: var(--color-surface-alt);
  font-size: 0.88rem;
  transition: border-color 0.15s ease, color 0.15s ease;
}
.dropzone:hover { border-color: var(--color-secondary-300); }
.dropzone.tem-ficheiro { border-color: var(--color-accent-500); color: var(--color-text); }

.erro { color: var(--color-danger); font-size: 0.85rem; }
.aviso { color: var(--color-warning); font-size: 0.85rem; }
.sucesso { color: var(--color-success); font-size: 0.85rem; }

.secao-titulo {
  max-width: 640px;
  margin: 0 auto 1rem;
}

.lista-documentos {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 640px;
  margin: 0 auto;
}

.documento-card { padding: 1.1rem 1.25rem; }

.documento-cabecalho {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.documento-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.documento-info strong { font-size: 0.92rem; color: var(--color-text); }
.documento-meta { font-size: 0.76rem; color: var(--color-text-muted); }

.documento-obs {
  font-size: 0.8rem;
  color: var(--color-text-soft);
  background: var(--color-primary-50);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.7rem;
  margin-top: 0.7rem;
}

.estado-badge {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0.3rem 0.6rem;
  border-radius: var(--radius-pill);
  flex-shrink: 0;
}
.estado-ok { background: var(--color-accent-100); color: var(--color-accent-600); }
.estado-erro { background: #fbeae6; color: var(--color-danger); }
.estado-processando { background: var(--color-primary-100); color: var(--color-primary-700); }
.estado-neutro { background: var(--color-secondary-100); color: var(--color-secondary-600); }

.alerta-protegido,
.alerta-falha {
  margin-top: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-sm);
  border: 1px solid #f0c4b8;
  background: #fff8f6;
  color: var(--color-text);
  font-size: 0.84rem;
  line-height: 1.45;
}
.alerta-protegido strong,
.alerta-falha strong {
  display: block;
  color: var(--color-danger);
  margin-bottom: 0.35rem;
  font-size: 0.88rem;
}
.alerta-protegido p,
.alerta-falha p {
  margin: 0 0 0.5rem;
}
.acoes-lista {
  margin: 0.4rem 0 0.5rem 1.1rem;
  padding: 0;
  color: var(--color-text-soft);
  font-size: 0.8rem;
}
.acoes-lista li { margin: 0.2rem 0; }
.alerta-hint {
  margin: 0.35rem 0 0 !important;
  font-size: 0.78rem !important;
  color: var(--color-text-muted) !important;
}

.documento-acoes {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 0.9rem;
  flex-wrap: wrap;
}

.select-diploma {
  max-width: 220px;
}

.btn-danger {
  background: transparent;
  color: var(--color-danger);
  border: 1.5px solid var(--color-danger);
}
.btn-danger:not(:disabled):hover {
  background: var(--color-danger);
  color: #fff;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(16, 22, 15, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 80;
  padding: 1rem;
}
.modal {
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal h3 { margin: 0 0 0.4rem; }
.modal-hint {
  font-size: 0.82rem;
  color: var(--color-text-muted);
  margin: 0 0 1rem;
}
.modal-acoes {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
  margin-top: 0.5rem;
}

.documento-obs.progresso {
  margin: 0.65rem 0 0.35rem;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-sm);
  background: var(--color-surface-alt, #f6f4ef);
  border: 1px solid var(--color-border);
  font-size: 0.82rem;
  color: var(--color-text-soft);
}
.progresso-cabecalho {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 0.4rem;
  line-height: 1.35;
}
.progresso-texto { flex: 1; }
.progresso-meta {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.progresso-hint {
  margin: 0.45rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
.progresso-bar {
  height: 7px;
  border-radius: 999px;
  background: var(--color-border);
  overflow: hidden;
}
.progresso-fill {
  height: 100%;
  min-width: 0;
  background: linear-gradient(90deg, var(--color-secondary-400, #6b9b6e), var(--color-secondary-600, #3d6b42));
  transition: width 0.35s ease;
}
</style>