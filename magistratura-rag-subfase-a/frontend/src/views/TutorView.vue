<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useTutorStore } from '@/stores/tutor'
import bibliotecaService from '@/services/bibliotecaService'
import { Send, Plus, Trash2, Sparkles, MessagesSquare, BookMarked, X, RotateCw } from 'lucide-vue-next'
import { BaseSelect } from '@/components/ui'
import FeatureArt from '@/components/brand/FeatureArt.vue'
import OuvirTexto from '@/components/audio/OuvirTexto.vue'

const tutor = useTutorStore()
const texto = ref('')
const painel = ref(null)

const diplomas = ref([])
const artigos = ref([])
const aCarregarDiplomas = ref(false)
const aCarregarArtigos = ref(false)
const diplomaSelecionado = ref('')
const artigoSelecionado = ref('')
const termoDiploma = ref('')

const sugestoes = [
  'O que é a presunção de inocência?',
  'Explica a diferença entre dolo e negligência',
  'Quais são os pressupostos da prisão preventiva?',
  'Resume os princípios gerais do processo civil'
]

/** Fonte seleccionada para o painel de citação interativa */
const fonteActiva = ref(null)

/**
 * Parte o texto da mensagem em segmentos de texto puro e marcadores [n].
 * @returns {{ type: 'text'|'cite', value: string, n?: number }[]}
 */
function segmentosComCitacoes(texto) {
  if (!texto) return []
  const re = /\[(\d+)\]/g
  const parts = []
  let last = 0
  let m
  while ((m = re.exec(texto)) !== null) {
    if (m.index > last) {
      parts.push({ type: 'text', value: texto.slice(last, m.index) })
    }
    parts.push({ type: 'cite', value: m[0], n: Number(m[1]) })
    last = m.index + m[0].length
  }
  if (last < texto.length) {
    parts.push({ type: 'text', value: texto.slice(last) })
  }
  return parts
}

function abrirFonte(mensagem, n) {
  const lista = mensagem?.fontes || []
  const f = lista.find((x) => Number(x.n) === Number(n))
  if (f) {
    fonteActiva.value = f
  } else {
    // Marcador sem metadados (histórico antigo) — mostra só o número
    fonteActiva.value = {
      n,
      diplomaTitulo: null,
      artigoNumero: null,
      extrato: 'Fonte citada na resposta, mas os metadados não estão disponíveis nesta mensagem.'
    }
  }
}

function fecharFonte() {
  fonteActiva.value = null
}

function rotuloFonte(f) {
  if (!f) return ''
  const art = f.artigoNumero ? `Art. ${f.artigoNumero}` : 'Fonte'
  const dip = f.diplomaTitulo || f.diplomaNumero || ''
  return dip ? `${art} — ${dip}` : art
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
  (diplomas.value || []).map((d) => ({ value: d.id, label: rotuloDiploma(d) }))
)
const opcoesArtigos = computed(() =>
  (artigos.value || []).map((a) => ({ value: a.id, label: rotuloArtigo(a) }))
)

async function carregarDiplomas(termo = '') {
  aCarregarDiplomas.value = true
  try {
    const params = { size: 500 }
    if ((termo || '').trim()) params.termo = termo.trim()
    const data = await bibliotecaService.listarDiplomas(params)
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
    const data = await bibliotecaService.listarArtigos({ diplomaId, size: 200 })
    artigos.value = data.content || data || []
  } catch {
    artigos.value = []
  } finally {
    aCarregarArtigos.value = false
  }
}

watch(diplomaSelecionado, async (id) => {
  artigoSelecionado.value = ''
  await carregarArtigos(id || undefined)
  tutor.definirContexto({
    diplomaId: id || null,
    artigoId: null
  })
})

watch(artigoSelecionado, (id) => {
  tutor.definirContexto({
    diplomaId: diplomaSelecionado.value || null,
    artigoId: id || null
  })
})

const contextoAtivo = computed(() => {
  const d = diplomas.value.find((x) => x.id === diplomaSelecionado.value)
  const a = artigos.value.find((x) => x.id === artigoSelecionado.value)
  if (!d && !a) return null
  return {
    diploma: d ? rotuloDiploma(d) : null,
    artigo: a ? rotuloArtigo(a) : null
  }
})

function limparContextoUi() {
  diplomaSelecionado.value = ''
  artigoSelecionado.value = ''
  artigos.value = []
  tutor.limparContexto()
}

onMounted(async () => {
  await Promise.all([tutor.carregarConversas(), carregarDiplomas()])
  tutor.verificarStatus()

  // Prefill vindo da página do artigo (Perguntar ao Tutor / explicar / exemplos)
  try {
    const raw =
      sessionStorage.getItem('tutor_prefill') ||
      sessionStorage.getItem('magistratura.tutor.prefill')
    if (raw) {
      sessionStorage.removeItem('tutor_prefill')
      sessionStorage.removeItem('magistratura.tutor.prefill')
      const pre = JSON.parse(raw)
      if (pre.diplomaId) {
        diplomaSelecionado.value = pre.diplomaId
        await carregarArtigos(pre.diplomaId)
      }
      if (pre.artigoId) {
        artigoSelecionado.value = pre.artigoId
      }
      tutor.definirContexto({
        diplomaId: pre.diplomaId || null,
        artigoId: pre.artigoId || null,
        topicoId: pre.topicoId || null
      })
      if (pre.mensagem) {
        texto.value = pre.mensagem
      }
    }
  } catch {
    // ignora prefill inválido
  }
})

async function nova() {
  await tutor.criarConversa()
}

async function selecionar(id) {
  await tutor.selecionarConversa(id)
  scrollParaFim()
}

async function eliminar(id, ev) {
  ev.stopPropagation()
  await tutor.eliminarConversa(id)
}

async function enviar() {
  const mensagem = texto.value.trim()
  if (!mensagem || tutor.aEnviar) return
  texto.value = ''
  await tutor.enviarMensagem(mensagem)
  scrollParaFim()
}

function usarSugestao(s) {
  texto.value = s
}

function scrollParaFim() {
  nextTick(() => {
    if (painel.value) painel.value.scrollTop = painel.value.scrollHeight
  })
}

const semConversa = computed(() => !tutor.conversaAtual)
</script>

<template>
  <div class="tutor-shell">
    <aside class="conversas">
      <div class="conversas-header">
        <FeatureArt variant="tutor" :size="28" />
        <h2>Conversas</h2>
        <button class="btn btn-secondary nova-btn" @click="nova">
          <Plus :size="16" /> Nova
        </button>
      </div>

      <div v-if="tutor.aCarregarConversas" class="center-state">
        <div class="spinner"></div>
      </div>

      <div v-else-if="!tutor.conversas.length" class="vazio-conversas">
        <MessagesSquare :size="26" />
        <p>Ainda não tens conversas. Cria a primeira.</p>
      </div>

      <ul v-else class="lista">
        <li
          v-for="c in tutor.conversas"
          :key="c.id"
          class="item"
          :class="{ ativo: tutor.conversaAtual?.id === c.id }"
          @click="selecionar(c.id)"
        >
          <span class="titulo">{{ c.titulo || 'Conversa sem título' }}</span>
          <button class="apagar" @click="eliminar(c.id, $event)"><Trash2 :size="14" /></button>
        </li>
      </ul>
    </aside>

    <section class="chat">
      <header class="chat-header">
        <div class="titulo-conversa">
          <FeatureArt variant="tutor" :size="28" />
          <span>{{ tutor.conversaAtual?.titulo || 'Tutor IA' }}</span>
        </div>
        <div class="status-ia">
          <span class="badge" :class="tutor.iaDisponivel ? 'badge-live' : 'badge-off'">
            {{ tutor.iaDisponivel ? tutor.providerNome || 'Ligado' : 'IA indisponível' }}
          </span>
          <span
            v-if="tutor.localLimitPerMinute != null && tutor.localRemaining != null"
            class="badge badge-quota"
            :class="{ 'badge-quota-low': tutor.localRemaining <= 3 }"
            :title="'Pedidos IA restantes nesta janela de 1 min'"
          >
            {{ tutor.localRemaining }}/{{ tutor.localLimitPerMinute }}
          </span>
          <span
            v-if="tutor.upstreamRateLimited"
            class="badge badge-limited"
            :title="tutor.retryAfterSeconds ? 'Aguarda ~' + tutor.retryAfterSeconds + 's' : 'Limite do fornecedor'"
          >
            limite API
          </span>
          <button
            v-if="!tutor.iaDisponivel || tutor.upstreamRateLimited"
            type="button"
            class="retry-status"
            title="Verificar novamente"
            @click="tutor.verificarStatus()"
          >
            <RotateCw :size="13" />
          </button>
        </div>
      </header>

      <!-- Contexto jurídico -->
      <div class="contexto-barra">
        <div class="contexto-campos">
          <BookMarked :size="16" class="contexto-icon" />
          <BaseSelect
            v-model="diplomaSelecionado"
            :options="opcoesDiplomas"
            placeholder="Diploma (opcional)"
            searchable
            search-placeholder="Filtrar diploma…"
            class="contexto-select"
          />
          <BaseSelect
            v-model="artigoSelecionado"
            :options="opcoesArtigos"
            placeholder="Artigo (opcional)"
            :disabled="!diplomaSelecionado"
            searchable
            class="contexto-select"
          />
          <button
            v-if="diplomaSelecionado || artigoSelecionado"
            type="button"
            class="btn-limpar-contexto"
            title="Limpar contexto"
            @click="limparContextoUi"
          >
            <X :size="14" />
          </button>
        </div>
        <p v-if="contextoAtivo" class="contexto-hint">
          O Tutor responde com base em
          <strong v-if="contextoAtivo.artigo">{{ contextoAtivo.artigo }}</strong>
          <template v-if="contextoAtivo.artigo && contextoAtivo.diploma"> · </template>
          <strong v-if="contextoAtivo.diploma && !contextoAtivo.artigo">{{
            contextoAtivo.diploma
          }}</strong>
          <span v-if="contextoAtivo.diploma && contextoAtivo.artigo">
            ({{ contextoAtivo.diploma }})</span
          >.
        </p>
        <p v-else class="contexto-hint muted">
          Sem contexto documental — o Tutor avisará que não está a citar uma fonte concreta da
          biblioteca.
        </p>
      </div>

      <div ref="painel" class="painel">
        <div v-if="semConversa && !tutor.mensagens.length" class="boas-vindas">
          <div class="icone"><FeatureArt variant="tutor" :size="40" /></div>
          <h3>Tutor Jurídico IA</h3>
          <p>
            Escolhe um diploma ou artigo acima para ancorar as respostas na legislação real da
            biblioteca. As tuas conversas ficam guardadas.
          </p>
          <div class="chips">
            <button v-for="s in sugestoes" :key="s" class="chip" @click="usarSugestao(s)">
              {{ s }}
            </button>
          </div>
        </div>

        <div
          v-for="m in tutor.mensagens"
          :key="m.id"
          class="mensagem"
          :class="m.autor === 'UTILIZADOR' ? 'do-utilizador' : 'da-ia'"
        >
          <div class="bolha">
            <template v-if="m.autor === 'IA'">
              <template v-for="(seg, i) in segmentosComCitacoes(m.conteudo)" :key="i">
                <button
                  v-if="seg.type === 'cite'"
                  type="button"
                  class="cite-mark"
                  :title="'Ver fonte ' + seg.n"
                  @click.stop="abrirFonte(m, seg.n)"
                >{{ seg.value }}</button>
                <span v-else>{{ seg.value }}</span>
              </template>
              <span v-if="m.aEscrever" class="cursor">▍</span>
              <div v-if="m.fontes?.length && !m.aEscrever" class="fontes-lista">
                <span class="fontes-label">Fontes:</span>
                <button
                  v-for="f in m.fontes"
                  :key="f.n"
                  type="button"
                  class="fonte-chip"
                  @click.stop="abrirFonte(m, f.n)"
                >[{{ f.n }}] {{ f.artigoNumero ? 'Art. ' + f.artigoNumero : 'Fonte' }}</button>
              </div>
              <div v-if="!m.aEscrever && m.conteudo" class="mensagem-audio">
                <OuvirTexto :texto="m.conteudo" label="Ouvir resposta" compacto />
              </div>
            </template>
            <template v-else>
              {{ m.conteudo }}
            </template>
          </div>
        </div>

        <div v-if="tutor.erro" class="erro-msg">{{ tutor.erro }}</div>
      </div>

      <!-- Painel de fonte citada -->
      <aside v-if="fonteActiva" class="fonte-painel" role="dialog" aria-label="Fonte jurídica">
        <header class="fonte-painel-header">
          <strong>[{{ fonteActiva.n }}] {{ rotuloFonte(fonteActiva) }}</strong>
          <button type="button" class="btn-fechar-fonte" @click="fecharFonte" aria-label="Fechar">
            <X :size="16" />
          </button>
        </header>
        <p v-if="fonteActiva.artigoTitulo" class="fonte-titulo">{{ fonteActiva.artigoTitulo }}</p>
        <p v-if="fonteActiva.capitulo || fonteActiva.seccao" class="fonte-meta">
          <span v-if="fonteActiva.capitulo">{{ fonteActiva.capitulo }}</span>
          <span v-if="fonteActiva.seccao"> · {{ fonteActiva.seccao }}</span>
        </p>
        <div class="fonte-extrato">{{ fonteActiva.extrato }}</div>
        <div v-if="fonteActiva.extrato" class="fonte-audio">
          <OuvirTexto :texto="fonteActiva.extrato" label="Ouvir fonte" compacto />
        </div>
        <router-link
          v-if="fonteActiva.artigoId"
          class="fonte-link"
          :to="{ name: 'artigo', params: { id: fonteActiva.artigoId } }"
          @click="fecharFonte"
        >
          Abrir artigo na Biblioteca →
        </router-link>
      </aside>

      <form class="compositor" @submit.prevent="enviar">
        <textarea
          v-model="texto"
          rows="1"
          placeholder="Escreve a tua pergunta ao Tutor IA…"
          @keydown.enter.exact.prevent="enviar"
        ></textarea>
        <button type="submit" class="btn btn-primary envio" :disabled="!texto.trim() || tutor.aEnviar">
          <Send :size="17" />
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.tutor-shell {
  display: flex;
  height: 100vh;
}

.conversas {
  width: 280px;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  padding: 1.25rem;
}

.conversas-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.conversas-header h2 {
  font-size: 1rem;
  margin: 0;
}

.nova-btn {
  padding: 0.4rem 0.8rem;
  font-size: 0.78rem;
}

.vazio-conversas {
  text-align: center;
  color: var(--color-text-muted);
  padding: 2rem 0.5rem;
  font-size: 0.85rem;
}

.lista {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.65rem 0.75rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.item:hover {
  background: var(--color-primary-50);
}

.item.ativo {
  background: var(--color-secondary-100);
  color: var(--color-secondary-700);
  font-weight: 600;
}

.titulo {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.apagar {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.item:hover .apagar {
  opacity: 1;
}

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.75rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
}

.titulo-conversa {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  color: var(--color-secondary-600);
  font-family: var(--font-heading);
}

.status-ia {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.retry-status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, transform 0.15s ease;
}

.retry-status:hover {
  color: var(--color-secondary-600);
  border-color: var(--color-secondary-300);
  transform: rotate(90deg);
}

.contexto-barra {
  padding: 0.75rem 1.75rem;
  background: var(--color-surface-alt);
  border-bottom: 1px solid var(--color-border);
}

.contexto-campos {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.contexto-icon {
  color: var(--color-secondary-500);
  flex-shrink: 0;
}

.contexto-select {
  min-width: 160px;
  max-width: 280px;
  font-size: 0.82rem;
  padding: 0.45rem 0.65rem;
}

.btn-limpar-contexto {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
}
.btn-limpar-contexto:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.contexto-hint {
  margin: 0.45rem 0 0;
  font-size: 0.76rem;
  color: var(--color-text-soft);
}
.contexto-hint.muted {
  color: var(--color-text-muted);
}

.painel {
  flex: 1;
  overflow-y: auto;
  padding: 2rem 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.boas-vindas {
  max-width: 480px;
  margin: 2rem auto;
  text-align: center;
}

.icone {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
  margin-top: 1.25rem;
}

.chip {
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  color: var(--color-secondary-600);
  padding: 0.5rem 0.9rem;
  border-radius: var(--radius-pill);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip:hover {
  border-color: var(--color-secondary-300);
  background: var(--color-secondary-100);
}

.mensagem {
  display: flex;
}

.do-utilizador {
  justify-content: flex-end;
}

.bolha {
  max-width: 68%;
  padding: 0.75rem 1.05rem;
  border-radius: var(--radius-md);
  font-size: 0.92rem;
  line-height: 1.5;
  white-space: pre-wrap;
}

.do-utilizador .bolha {
  background: var(--color-secondary-500);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.da-ia .bolha {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text);
  border-bottom-left-radius: 4px;
}

.cursor {
  animation: blink 0.9s steps(1) infinite;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}

.erro-msg {
  text-align: center;
  color: var(--color-danger);
  font-size: 0.85rem;
}

.compositor {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  padding: 1rem 1.75rem 1.5rem;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.compositor textarea {
  flex: 1;
  resize: none;
  max-height: 140px;
}

.envio {
  border-radius: 50%;
  width: 44px;
  height: 44px;
  padding: 0;
  flex-shrink: 0;
}

@media (max-width: 880px) {
  .tutor-shell {
    flex-direction: column;
    height: auto;
    min-height: calc(100vh - 64px);
  }
  .conversas {
    width: 100%;
    max-height: 180px;
  }
  .contexto-select {
    max-width: 100%;
    min-width: 0;
    flex: 1;
  }
}

/* --- Citações interactivas --- */
.cite-mark {
  display: inline;
  padding: 0 0.2rem;
  margin: 0 0.05rem;
  border: none;
  border-radius: 4px;
  background: var(--color-secondary-100);
  color: var(--color-secondary-700);
  font-weight: 700;
  font-size: 0.82em;
  cursor: pointer;
  vertical-align: baseline;
  line-height: inherit;
  font-family: inherit;
}
.cite-mark:hover {
  background: var(--color-secondary-300);
  color: var(--color-secondary-900);
}

.fontes-lista {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  align-items: center;
  margin-top: 0.65rem;
  padding-top: 0.55rem;
  border-top: 1px dashed var(--color-border);
}
.fontes-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
  font-weight: 600;
}
.fonte-chip {
  border: 1px solid var(--color-border);
  background: var(--color-surface-alt, var(--color-bg));
  color: var(--color-secondary-700);
  border-radius: var(--radius-pill, 999px);
  padding: 0.2rem 0.55rem;
  font-size: 0.72rem;
  cursor: pointer;
  font-family: inherit;
}
.fonte-chip:hover {
  border-color: var(--color-secondary-400);
  background: var(--color-secondary-100);
}

.fonte-painel {
  position: absolute;
  right: 1rem;
  bottom: 5.5rem;
  width: min(360px, calc(100% - 2rem));
  max-height: 42vh;
  overflow-y: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 12px);
  box-shadow: var(--shadow-lg);
  padding: 0.9rem 1rem 1rem;
  z-index: 20;
}
.fonte-painel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}
.fonte-painel-header strong {
  font-size: 0.9rem;
  color: var(--color-secondary-700);
}
.btn-fechar-fonte {
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.15rem;
  display: flex;
}
.fonte-titulo {
  margin: 0 0 0.25rem;
  font-size: 0.85rem;
  font-weight: 600;
}
.fonte-meta {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
.fonte-extrato {
  font-size: 0.82rem;
  line-height: 1.45;
  white-space: pre-wrap;
  color: var(--color-text);
  background: var(--color-surface-alt, var(--color-bg));
  border-radius: var(--radius-sm, 8px);
  padding: 0.65rem 0.75rem;
}
.fonte-link {
  display: inline-block;
  margin-top: 0.65rem;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-secondary-600);
  text-decoration: none;
}
.fonte-link:hover {
  text-decoration: underline;
}

.mensagem-audio {
  margin-top: 0.55rem;
  padding-top: 0.45rem;
  border-top: 1px dashed var(--color-border);
}
.fonte-audio {
  margin-top: 0.55rem;
}
.chat {
  position: relative;
}
</style>
