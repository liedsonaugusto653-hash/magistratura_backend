<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useOntologiaStore } from '@/stores/ontologia'
import {
  Network,
  ArrowLeft,
  BookOpen,
  Sparkles,
  Link2,
  ChevronRight,
  Search,
  HelpCircle,
  ArrowDownToLine,
  ArrowUpFromLine,
  Lightbulb,
  MapPin,
  AlertTriangle,
  Eye
} from 'lucide-vue-next'
import { LoadingState, EmptyState, BaseButton } from '@/components/ui'
import { PageHero } from '@/components/ui'

const store = useOntologiaStore()
const router = useRouter()
const termoPesquisa = ref('')
const resultadosPesquisa = ref([])
const aPesquisar = ref(false)

const aSincronizar = ref(false)
const msgSync = ref('')

onMounted(() => store.carregarEntidades())

async function sincronizarBiblioteca() {
  msgSync.value = ''
  aSincronizar.value = true
  try {
    const { default: ontologiaService } = await import('@/services/ontologiaService')
    // garantir método
    const http = (await import('@/api/http')).default
    const { data } = await http.post('/ontologia/auto-ligar-biblioteca', null, { params: { dryRun: false } })
    const lista = Array.isArray(data) ? data : []
    const criadas = lista.filter((x) => x.estado === 'CRIADA').length
    const existentes = lista.filter((x) => x.estado === 'JA_EXISTIA').length
    msgSync.value = `Ligações: ${criadas} novas, ${existentes} já existiam (${lista.length} no total).`
    // refrescar mapa se aberto
    if (store.mapa?.entidade?.id) {
      await store.abrirMapa(store.mapa.entidade.id)
      if (store.topicoActivo?.id) await store.seleccionarTopico(store.topicoActivo.id)
    } else {
      await store.carregarEntidades()
    }
  } catch (e) {
    msgSync.value =
      e.response?.data?.mensagem ||
      e.response?.data?.message ||
      'Falha ao sincronizar com a biblioteca.'
  } finally {
    aSincronizar.value = false
  }
}

const entidadesFiltradas = computed(() => {
  const t = termoPesquisa.value.trim().toLowerCase()
  if (!t) return store.entidades
  return (store.entidades || []).filter(
    (e) =>
      e.nome?.toLowerCase().includes(t) ||
      e.codigo?.toLowerCase().includes(t) ||
      e.descricao?.toLowerCase().includes(t)
  )
})

async function abrirEntidade(id) {
  await store.abrirMapa(id)
}

function voltarLista() {
  store.limparMapa()
}

async function seleccionarTopico(id) {
  await store.seleccionarTopico(id)
}

function abrirArtigo(artigoId) {
  if (!artigoId) return
  router.push({ name: 'artigo', params: { id: artigoId } })
}

function perguntarTutor(topico) {
  if (!topico) return
  try {
    sessionStorage.setItem(
      'tutor_prefill',
      JSON.stringify({
        topicoId: topico.id,
        mensagem: `Explica o conceito jurídico «${topico.nome}» com base na legislação angolana da biblioteca.`
      })
    )
  } catch {
    /* ignore */
  }
  router.push({ name: 'tutor' })
}

async function pesquisarConceitos() {
  const t = termoPesquisa.value.trim()
  if (!t || t.length < 2) {
    resultadosPesquisa.value = []
    return
  }
  aPesquisar.value = true
  try {
    const { default: ontologiaService } = await import('@/services/ontologiaService')
    resultadosPesquisa.value = await ontologiaService.pesquisarTopicos(t)
  } catch {
    resultadosPesquisa.value = []
  } finally {
    aPesquisar.value = false
  }
}

async function irParaTopicoPesquisa(t) {
  if (t.entidadeId) {
    await store.abrirMapa(t.entidadeId)
    await store.seleccionarTopico(t.id)
    resultadosPesquisa.value = []
    termoPesquisa.value = ''
  }
}

// --- Ficha de Estudo (definição + perguntas-guia) ---------------------------

const temFichaEstudo = computed(() => !!store.topicoActivo?.definicaoEstudo)

function posicaoTrilha(topicoId) {
  const item = store.trilha.find((t) => t.topicoId === topicoId)
  return item ? item.posicao : null
}

const mostrarExplicacaoCaso = ref(false)
watch(
  () => store.topicoActivo?.id,
  () => {
    mostrarExplicacaoCaso.value = false
  }
)

async function gerarFicha(forcar) {
  if (!store.topicoActivo) return
  try {
    await store.gerarFichaEstudo(store.topicoActivo.id, forcar)
  } catch {
    /* mensagem já fica disponível em store.erroFicha */
  }
}

// --- Agrupamento das relações conceptuais para estudo -----------------------
// PRESSUPOE/ESPECIALIZA distinguem o que é base (pré-requisito) do que
// depende deste conceito (consequência/desdobramento); o resto fica "relacionado".

const TIPOS_HIERARQUICOS = ['PRESSUPOE', 'ESPECIALIZA']

const preRequisitos = computed(() =>
  (store.topicoActivo?.relacoes || []).filter(
    (r) => TIPOS_HIERARQUICOS.includes(r.tipoRelacao) && r.outgoing
  )
)

const consequencias = computed(() =>
  (store.topicoActivo?.relacoes || []).filter(
    (r) => TIPOS_HIERARQUICOS.includes(r.tipoRelacao) && !r.outgoing
  )
)

const relacionados = computed(() =>
  (store.topicoActivo?.relacoes || []).filter((r) => !TIPOS_HIERARQUICOS.includes(r.tipoRelacao))
)
</script>

<template>
  <div class="page">
    <PageHero
      eyebrow="Conhecimento"
      title="Mapa Jurídico"
      lead="Navega relações entre diplomas, artigos e temas."
      art="mapa"
    />

    <!-- Pesquisa global de tópicos -->
    <div class="pesquisa-barra card">
      <Search :size="18" class="pesquisa-icon" />
      <input
        v-model="termoPesquisa"
        type="search"
        placeholder="Pesquisar conceito (ex.: prisão preventiva, empregador…)"
        @keyup.enter="pesquisarConceitos"
        @input="termoPesquisa.length >= 2 ? pesquisarConceitos() : (resultadosPesquisa = [])"
      />
      <BaseButton variant="secondary" :loading="aPesquisar" @click="pesquisarConceitos">
        Pesquisar
      </BaseButton>
    </div>

    <ul v-if="resultadosPesquisa.length" class="resultados-pesquisa card">
      <li
        v-for="t in resultadosPesquisa"
        :key="t.id"
        class="resultado-item"
        @click="irParaTopicoPesquisa(t)"
      >
        <span class="resultado-codigo">{{ t.codigo }}</span>
        <strong>{{ t.nome }}</strong>
        <span v-if="t.entidadeNome" class="resultado-ent">{{ t.entidadeNome }}</span>
        <ChevronRight :size="16" />
      </li>
    </ul>

    <LoadingState v-if="store.carregando" message="A carregar entidades jurídicas…" />
    <p v-else-if="store.erro && !store.mapa" class="erro">{{ store.erro }}</p>

    <!-- LISTA DE ENTIDADES -->
    <template v-else-if="!store.mapa">
      <div class="grid entidades-grid">
        <button
          v-for="e in entidadesFiltradas"
          :key="e.id"
          type="button"
          class="card entidade-card"
          @click="abrirEntidade(e.id)"
        >
          <span class="entidade-codigo">{{ e.codigo }}</span>
          <h2>{{ e.nome }}</h2>
          <p>{{ e.descricao || 'Conceito jurídico fundamental' }}</p>
          <span class="entidade-meta">{{ e.totalTopicos }} tópico(s)</span>
        </button>
      </div>
      <EmptyState
        v-if="!entidadesFiltradas.length && !store.carregando"
        title="Nenhuma entidade encontrada"
        description="Aplica a migration V28 ou ajusta a pesquisa."
      />
    </template>

    <!-- MAPA DE UMA ENTIDADE -->
    <template v-else>
      <div class="mapa-toolbar">
        <button type="button" class="btn-voltar" @click="voltarLista">
          <ArrowLeft :size="16" /> Todas as entidades
        </button>
      </div>

      <LoadingState v-if="store.carregandoMapa" message="A montar o mapa conceptual…" />

      <template v-else>
        <header class="mapa-header card">
          <span class="entidade-codigo">{{ store.mapa.entidade?.codigo }}</span>
          <h2>{{ store.mapa.entidade?.nome }}</h2>
          <p>{{ store.mapa.entidade?.descricao }}</p>
        </header>

        <div class="mapa-layout">
          <aside class="topicos-lista card">
            <h3>Tópicos</h3>
            <button
              v-for="t in store.mapa.topicos || []"
              :key="t.id"
              type="button"
              class="topico-item"
              :class="{ activo: store.topicoActivo?.id === t.id }"
              @click="seleccionarTopico(t.id)"
            >
              <span v-if="posicaoTrilha(t.id)" class="topico-posicao">{{ posicaoTrilha(t.id) }}</span>
              <span class="topico-nome">{{ t.nome }}</span>
              <span class="topico-badge">{{ t.totalArtigos }} art.</span>
            </button>
            <EmptyState
              v-if="!(store.mapa.topicos || []).length"
              title="Sem tópicos"
              description="Esta entidade ainda não tem tópicos no seed."
            />
          </aside>

          <section class="topico-detalhe card">
            <template v-if="store.topicoActivo">
              <div class="detalhe-cabecalho">
                <div>
                  <span class="resultado-codigo">{{ store.topicoActivo.codigo }}</span>
                  <h3>{{ store.topicoActivo.nome }}</h3>
                  <p v-if="store.topicoActivo.descricao">{{ store.topicoActivo.descricao }}</p>
                </div>
                <BaseButton variant="primary" @click="perguntarTutor(store.topicoActivo)">
                  <Sparkles :size="16" /> Perguntar ao Tutor
                </BaseButton>
              </div>

              <div class="ficha-estudo">
                <div class="ficha-cabecalho">
                  <h4><Sparkles :size="14" /> Ficha de Estudo</h4>
                  <BaseButton
                    variant="secondary"
                    :loading="store.aGerarFicha"
                    @click="gerarFicha(temFichaEstudo)"
                  >
                    {{ temFichaEstudo ? 'Atualizar ficha' : 'Gerar ficha de estudo' }}
                  </BaseButton>
                </div>

                <p v-if="store.erroFicha" class="erro-ficha">{{ store.erroFicha }}</p>

                <template v-if="temFichaEstudo">
                  <p class="ficha-definicao">{{ store.topicoActivo.definicaoEstudo }}</p>

                  <div v-if="store.topicoActivo.porqueExiste" class="ficha-bloco">
                    <h5><Lightbulb :size="13" /> Porque existe</h5>
                    <p>{{ store.topicoActivo.porqueExiste }}</p>
                  </div>

                  <div v-if="store.topicoActivo.ondeApareceVida?.length" class="ficha-bloco">
                    <h5><MapPin :size="13" /> Onde aparece na vida</h5>
                    <ul class="lista-simples">
                      <li v-for="(ex, i) in store.topicoActivo.ondeApareceVida" :key="i">{{ ex }}</li>
                    </ul>
                  </div>

                  <div v-if="store.topicoActivo.casoPratico" class="ficha-bloco caso-pratico">
                    <h5><HelpCircle :size="13" /> Caso prático — pensa antes de ver a resposta</h5>
                    <p class="caso-enunciado">{{ store.topicoActivo.casoPratico.enunciado }}</p>
                    <ul v-if="store.topicoActivo.casoPratico.perguntas?.length" class="lista-simples">
                      <li v-for="(q, i) in store.topicoActivo.casoPratico.perguntas" :key="i">{{ q }}</li>
                    </ul>
                    <BaseButton
                      v-if="!mostrarExplicacaoCaso && store.topicoActivo.casoPratico.explicacao"
                      variant="secondary"
                      @click="mostrarExplicacaoCaso = true"
                    >
                      <Eye :size="14" /> Ver explicação
                    </BaseButton>
                    <p v-else-if="store.topicoActivo.casoPratico.explicacao" class="caso-explicacao">
                      {{ store.topicoActivo.casoPratico.explicacao }}
                    </p>
                  </div>

                  <div v-if="store.topicoActivo.errosComuns?.length" class="ficha-bloco">
                    <h5><AlertTriangle :size="13" /> Erros comuns</h5>
                    <ul class="lista-simples">
                      <li v-for="(erro, i) in store.topicoActivo.errosComuns" :key="i">{{ erro }}</li>
                    </ul>
                  </div>

                  <div class="ficha-bloco">
                    <h5><HelpCircle :size="13" /> Scanner mental</h5>
                    <ol class="perguntas-guia">
                      <li v-for="(p, i) in store.topicoActivo.perguntasGuia" :key="i">
                        <div class="pergunta">
                          <HelpCircle :size="14" />
                          <strong>{{ p.pergunta }}</strong>
                        </div>
                        <p class="resposta">{{ p.resposta }}</p>
                      </li>
                    </ol>
                  </div>
                </template>
                <p v-else-if="!store.aGerarFicha" class="hint">
                  Ainda não há ficha de estudo para este conceito: gera a definição, o porquê,
                  exemplos do quotidiano, erros comuns, um caso prático e as 6 perguntas-guia de
                  raciocínio jurídico a partir dos artigos já ligados.
                </p>
              </div>


              <div v-if="preRequisitos.length || consequencias.length || relacionados.length" class="relacoes">
                <div v-if="preRequisitos.length" class="relacoes-grupo">
                  <h4><ArrowDownToLine :size="14" /> Precisas de saber primeiro</h4>
                  <ul>
                    <li v-for="r in preRequisitos" :key="r.id">
                      <strong>{{ r.topicoNome }}</strong>
                      <span v-if="r.notas" class="rel-notas">{{ r.notas }}</span>
                    </li>
                  </ul>
                </div>

                <div v-if="consequencias.length" class="relacoes-grupo">
                  <h4><ArrowUpFromLine :size="14" /> Desdobra-se em / depende disto</h4>
                  <ul>
                    <li v-for="r in consequencias" :key="r.id">
                      <strong>{{ r.topicoNome }}</strong>
                      <span v-if="r.notas" class="rel-notas">{{ r.notas }}</span>
                    </li>
                  </ul>
                </div>

                <div v-if="relacionados.length" class="relacoes-grupo">
                  <h4><Link2 :size="14" /> Outras relações conceptuais</h4>
                  <ul>
                    <li v-for="r in relacionados" :key="r.id">
                      <span class="rel-tipo">{{ r.tipoRelacao }}</span>
                      <span>{{ r.outgoing ? '→' : '←' }}</span>
                      <strong>{{ r.topicoNome }}</strong>
                      <span v-if="r.notas" class="rel-notas">{{ r.notas }}</span>
                    </li>
                  </ul>
                </div>
              </div>

              <div class="artigos-sec">
                <h4><BookOpen :size="14" /> Onde isto está na lei</h4>
                <p v-if="!store.artigosTopico.length" class="hint">
                  Ainda não há artigos ligados a este tópico. Processa PDFs na Biblioteca e usa
                  <code>POST /api/ontologia/topicos/{id}/artigos</code> (ou a UI de ligação futura)
                  para popular o mapa.
                </p>
                <ul v-else class="artigos-lista">
                  <li
                    v-for="a in store.artigosTopico"
                    :key="a.ligacaoId"
                    class="artigo-item"
                    @click="abrirArtigo(a.artigoId)"
                  >
                    <strong>Art. {{ a.artigoNumero }}</strong>
                    <span v-if="a.artigoTitulo"> — {{ a.artigoTitulo }}</span>
                    <span class="diploma-ref">{{ a.diplomaNumero || a.diplomaTitulo }}</span>
                  </li>
                </ul>
              </div>

              <div
                v-if="store.topicoActivo.topicoAnteriorId || store.topicoActivo.topicoSeguinteId"
                class="trilha-nav"
              >
                <span v-if="store.topicoActivo.totalTrilha" class="trilha-posicao">
                  Passo {{ store.topicoActivo.posicaoTrilha }} de {{ store.topicoActivo.totalTrilha }}
                  na trilha sugerida
                </span>
                <div class="trilha-botoes">
                  <BaseButton
                    v-if="store.topicoActivo.topicoAnteriorId"
                    variant="secondary"
                    @click="seleccionarTopico(store.topicoActivo.topicoAnteriorId)"
                  >
                    <ArrowLeft :size="14" /> {{ store.topicoActivo.topicoAnteriorNome }}
                  </BaseButton>
                  <BaseButton
                    v-if="store.topicoActivo.topicoSeguinteId"
                    variant="primary"
                    @click="seleccionarTopico(store.topicoActivo.topicoSeguinteId)"
                  >
                    Próximo conceito sugerido: {{ store.topicoActivo.topicoSeguinteNome }}
                    <ChevronRight :size="14" />
                  </BaseButton>
                </div>
              </div>
            </template>
            <EmptyState
              v-else
              title="Selecciona um tópico"
              description="Escolhe um conceito à esquerda para ver artigos e relações."
            />
          </section>
        </div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.titulo-icon {
  vertical-align: -4px;
  color: var(--color-secondary-500);
  margin-right: 0.35rem;
}

.pesquisa-barra {
  max-width: 720px;
  margin: 0 auto 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 1rem;
}
.pesquisa-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.pesquisa-barra input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.95rem;
  outline: none;
}

.resultados-pesquisa {
  max-width: 720px;
  margin: 0 auto 1.5rem;
  padding: 0.5rem;
  list-style: none;
}
.resultado-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.9rem;
}
.resultado-item:hover {
  background: var(--color-primary-50, #f3f6f1);
}
.resultado-codigo {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-secondary-600);
  background: var(--color-secondary-100);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}
.resultado-ent {
  margin-left: auto;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.entidades-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
  max-width: 1100px;
  margin: 0 auto;
}
.entidade-card {
  text-align: left;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  padding: 1.15rem 1.25rem;
}
.entidade-card:hover {
  border-color: var(--color-secondary-300);
  box-shadow: var(--shadow-md);
}
.entidade-card h2 {
  margin: 0.35rem 0 0.4rem;
  font-size: 1.15rem;
}
.entidade-card p {
  margin: 0;
  font-size: 0.84rem;
  color: var(--color-text-muted);
  line-height: 1.4;
}
.entidade-codigo {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-secondary-600);
}
.entidade-meta {
  display: inline-block;
  margin-top: 0.75rem;
  font-size: 0.75rem;
  color: var(--color-text-soft);
}

.mapa-toolbar {
  max-width: 1100px;
  margin: 0 auto 0.75rem;
}
.btn-voltar {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: none;
  background: transparent;
  color: var(--color-secondary-600);
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
  padding: 0.35rem 0;
}
.btn-voltar:hover {
  text-decoration: underline;
}

.mapa-header {
  max-width: 1100px;
  margin: 0 auto 1.25rem;
  padding: 1.25rem 1.5rem;
}
.mapa-header h2 {
  margin: 0.25rem 0 0.35rem;
}

.mapa-layout {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(200px, 280px) 1fr;
  gap: 1.25rem;
  align-items: start;
}

.topicos-lista {
  padding: 1rem;
  position: sticky;
  top: 1rem;
}
.topicos-lista h3 {
  margin: 0 0 0.75rem;
  font-size: 0.95rem;
}
.topico-posicao {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.35rem;
  height: 1.35rem;
  padding: 0 0.3rem;
  border-radius: 999px;
  background: var(--color-primary-100, #e4ebe0);
  color: var(--color-primary-700, var(--color-primary-600));
  font-size: 0.72rem;
  font-weight: 600;
  flex-shrink: 0;
}

.trilha-nav {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.trilha-posicao {
  font-size: 0.78rem;
  color: var(--color-text-soft);
}
.trilha-botoes {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.topico-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
  text-align: left;
  padding: 0.55rem 0.65rem;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  font-size: 0.86rem;
  color: var(--color-text);
  margin-bottom: 0.2rem;
}
.topico-item:hover {
  background: var(--color-primary-50, #f3f6f1);
}
.topico-item.activo {
  background: var(--color-secondary-100);
  color: var(--color-secondary-800);
  font-weight: 600;
}
.topico-badge {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.topico-detalhe {
  padding: 1.25rem 1.4rem;
  min-height: 280px;
}
.detalhe-cabecalho {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}
.detalhe-cabecalho h3 {
  margin: 0.25rem 0 0.35rem;
}

.ficha-estudo {
  margin-bottom: 1.5rem;
  padding: 1rem 1.1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-primary-50, #f6f8f4);
}
.ficha-cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}
.ficha-cabecalho h4 {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9rem;
  margin: 0;
  color: var(--color-text-soft);
}
.ficha-definicao {
  font-size: 0.92rem;
  line-height: 1.5;
  margin: 0 0 1rem;
}
.ficha-bloco {
  margin-bottom: 1rem;
}
.ficha-bloco h5 {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  color: var(--color-text-soft);
  margin: 0 0 0.4rem;
}
.ficha-bloco p {
  font-size: 0.88rem;
  line-height: 1.5;
  margin: 0;
}
.lista-simples {
  margin: 0;
  padding-left: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.87rem;
}
.caso-pratico {
  padding: 0.75rem 0.9rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.caso-enunciado {
  margin-bottom: 0.5rem;
}
.caso-explicacao {
  margin-top: 0.6rem;
  padding-top: 0.6rem;
  border-top: 1px dashed var(--color-border);
  font-style: italic;
}
.erro-ficha {
  color: var(--color-danger);
  font-size: 0.85rem;
  margin: 0 0 0.75rem;
}
.perguntas-guia {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}
.perguntas-guia li {
  padding: 0.6rem 0.75rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.perguntas-guia .pergunta {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.86rem;
  color: var(--color-secondary-700, var(--color-secondary-600));
}
.perguntas-guia .resposta {
  margin: 0.3rem 0 0 1.35rem;
  font-size: 0.85rem;
  color: var(--color-text);
}

.relacoes {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}
.relacoes-grupo h4,
.artigos-sec h4 {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.88rem;
  margin: 0 0 0.65rem;
  color: var(--color-text-soft);
}
.relacoes-grupo ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.relacoes-grupo li {
  font-size: 0.85rem;
  padding: 0.4rem 0;
  border-bottom: 1px dashed var(--color-border);
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.5rem;
  align-items: baseline;
}
.rel-tipo {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: var(--color-primary-100, #e8efe4);
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
}
.rel-notas {
  width: 100%;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
.hint code {
  font-size: 0.78rem;
}

.artigos-lista {
  list-style: none;
  margin: 0;
  padding: 0;
}
.artigo-item {
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 0.5rem;
  cursor: pointer;
  font-size: 0.88rem;
  transition: border-color 0.15s ease;
}
.artigo-item:hover {
  border-color: var(--color-secondary-400);
  background: var(--color-surface-alt, var(--color-bg));
}
.diploma-ref {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.erro {
  color: var(--color-danger);
  text-align: center;
}

@media (max-width: 800px) {
  .mapa-layout {
    grid-template-columns: 1fr;
  }
  .topicos-lista {
    position: static;
  }
}
.sync-barra {
  max-width: 720px;
  margin: 0.75rem auto 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
}
.sync-msg {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-soft);
}
</style>
