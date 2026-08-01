<script setup>
import PageHint from '@/components/ui/PageHint.vue'
import { confirmarEliminacao } from '@/utils/prefsUi'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useFlashcardStore } from '@/stores/flashcard'
import bibliotecaService from '@/services/bibliotecaService'
import { Check, X, ChevronLeft, ChevronRight, Layers, Plus, Pencil } from 'lucide-vue-next'
import { EmptyState, LoadingState, ErrorState, BaseButton, BaseSelect } from '@/components/ui'
import { PageHero } from '@/components/ui'

const store = useFlashcardStore()
const router = useRouter()
const virado = ref(false)
const modo = ref('revisao') // revisao | formulario
const editandoId = ref(null)
const formErro = ref('')
const diplomas = ref([])

const form = reactive({
  pergunta: '',
  resposta: '',
  diplomaId: ''
})

onMounted(async () => {
  await store.carregar()
  try {
    const data = await bibliotecaService.listarDiplomas({ size: 200 })
    diplomas.value = data.content || data || []
  } catch {
    diplomas.value = []
  }
})

const opcoesDiplomas = () =>
  (diplomas.value || []).map((d) => ({
    value: d.id,
    label: d.numero ? `${d.numero} — ${d.titulo}` : d.titulo || 'Diploma'
  }))

async function responder(acertou) {
  await store.revisar(acertou)
}

function seguinte() {
  virado.value = false
  store.seguinte()
}
function anterior() {
  virado.value = false
  store.anterior()
}

async function eliminarCartao() {
  if (!store.cartaoAtual) return
  if (!confirmarEliminacao('Eliminar este flashcard?')) return
  try {
    await store.eliminar(store.cartaoAtual.id)
    virado.value = false
  } catch (_) {}
}

function irGerar() {
  router.push({ name: 'ferramentas', query: { aba: 'flashcards' } })
}

function abrirNovo() {
  editandoId.value = null
  form.pergunta = ''
  form.resposta = ''
  form.diplomaId = ''
  formErro.value = ''
  modo.value = 'formulario'
}

function abrirEditar() {
  const c = store.cartaoAtual
  if (!c) return
  editandoId.value = c.id
  form.pergunta = c.pergunta || ''
  form.resposta = c.resposta || ''
  form.diplomaId = c.diplomaId || ''
  formErro.value = ''
  modo.value = 'formulario'
}

function cancelarForm() {
  modo.value = 'revisao'
  formErro.value = ''
}

async function guardarForm() {
  formErro.value = ''
  if (!form.pergunta.trim() || !form.resposta.trim()) {
    formErro.value = 'Preenche a pergunta e a resposta.'
    return
  }
  const payload = {
    pergunta: form.pergunta.trim(),
    resposta: form.resposta.trim(),
    diplomaId: form.diplomaId || undefined
  }
  try {
    if (editandoId.value) {
      await store.actualizar(editandoId.value, payload)
    } else {
      await store.criar(payload)
    }
    modo.value = 'revisao'
    virado.value = false
  } catch (_) {
    formErro.value = store.erro || 'Não foi possível guardar.'
  }
}
</script>

<template>
  <div class="page">
    <PageHero
      eyebrow="Flashcards"
      title="Revisão de Flashcards"
      lead="Memória activa: relembra, verifica, reforça."
      art="flashcards"
    />
    <PageHint text="Revê cartões para fixar conceitos. Marca se acertaste para acompanhar o progresso." />


    <div class="acoes-topo">
      <BaseButton variant="secondary" @click="abrirNovo">
        <Plus :size="16" /> Novo cartão
      </BaseButton>
      <BaseButton variant="ghost" @click="irGerar">
        <Layers :size="16" /> Gerar com IA
      </BaseButton>
    </div>

    <!-- Formulário criar / editar -->
    <form v-if="modo === 'formulario'" class="card form-manual" @submit.prevent="guardarForm">
      <h2>{{ editandoId ? 'Editar flashcard' : 'Novo flashcard' }}</h2>
      <div class="field">
        <label>Pergunta *</label>
        <textarea v-model="form.pergunta" rows="3" required maxlength="4000" placeholder="Ex.: O que é a presunção de inocência?" />
      </div>
      <div class="field">
        <label>Resposta *</label>
        <textarea v-model="form.resposta" rows="3" required maxlength="4000" placeholder="Resposta correcta…" />
      </div>
      <div class="field">
        <label>Diploma (opcional)</label>
        <BaseSelect
          v-model="form.diplomaId"
          :options="opcoesDiplomas()"
          placeholder="— Nenhum —"
          searchable
        />
      </div>
      <p v-if="formErro" class="erro">{{ formErro }}</p>
      <div class="form-acoes">
        <BaseButton type="submit" variant="primary" :loading="store.aGuardar" loading-text="A guardar…">
          Guardar
        </BaseButton>
        <BaseButton type="button" variant="ghost" @click="cancelarForm">Cancelar</BaseButton>
      </div>
    </form>

    <template v-else>
      <LoadingState v-if="store.carregando" message="A carregar flashcards…" />
      <ErrorState v-else-if="store.erro && !store.lista.length" :message="store.erro" @retry="store.carregar()" />

      <EmptyState
        v-else-if="!store.lista.length"
        title="Ainda não existem flashcards"
        description="Cria um cartão manualmente ou gera a partir da legislação com IA."
        :icon="Layers"
      >
        <BaseButton variant="primary" @click="abrirNovo">
          <Plus :size="16" /> Criar flashcard
        </BaseButton>
        <BaseButton variant="secondary" @click="irGerar">
          <Layers :size="16" /> Gerar com IA
        </BaseButton>
      </EmptyState>

      <template v-else>
        <p style="text-align: center; color: var(--color-text-muted); margin-bottom: 1rem">
          {{ store.indice + 1 }} / {{ store.total }}
          ·
          <button type="button" class="btn btn-ghost" style="font-size: 0.8rem" @click="abrirEditar">
            <Pencil :size="12" style="vertical-align: -1px" /> Editar
          </button>
          ·
          <button type="button" class="btn btn-ghost" style="font-size: 0.8rem" @click="eliminarCartao">
            Eliminar
          </button>
        </p>

        <div class="flashcard" :class="{ virado }" @click="virado = !virado">
          <div class="flashcard-inner" :style="virado ? 'transform:rotateY(180deg)' : ''">
            <div class="face frente card">
              <span class="face-label">Pergunta</span>
              <p>{{ store.cartaoAtual?.pergunta }}</p>
            </div>
            <div class="face verso card">
              <span class="face-label">Resposta</span>
              <p>{{ store.cartaoAtual?.resposta }}</p>
            </div>
          </div>
        </div>

        <div v-if="store.resultado" class="card card-static resultado">
          <p :class="store.resultado.acertou ? 'ok' : 'ko'">
            {{ store.resultado.acertou ? 'Correto!' : 'Incorreto' }}
          </p>
          <p class="meta">
            {{ store.resultado.acertos }} acertos · {{ store.resultado.erros }} erros ·
            {{ Math.round(store.resultado.percentagemAcerto || 0) }}%
          </p>
        </div>

        <div class="nav-acoes">
          <BaseButton variant="ghost" :disabled="store.indice === 0" @click="anterior">
            <ChevronLeft :size="16" /> Anterior
          </BaseButton>
          <BaseButton variant="secondary" @click="responder(true)">
            <Check :size="16" /> Acertei
          </BaseButton>
          <BaseButton variant="ghost" @click="responder(false)">
            <X :size="16" /> Errei
          </BaseButton>
          <BaseButton variant="ghost" :disabled="store.indice >= store.total - 1" @click="seguinte">
            Seguinte <ChevronRight :size="16" />
          </BaseButton>
        </div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.acoes-topo {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}
.form-manual {
  max-width: 560px;
  margin: 0 auto 1.5rem;
  padding: 1.35rem 1.5rem;
}
.form-manual h2 {
  margin: 0 0 1rem;
  font-size: 1.05rem;
}
.form-acoes {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.5rem;
}
.erro {
  color: var(--color-danger);
  font-size: 0.85rem;
}
.flashcard {
  perspective: 1000px;
  max-width: 480px;
  margin: 0 auto 1.25rem;
  min-height: 220px;
  cursor: pointer;
}
.flashcard-inner {
  position: relative;
  width: 100%;
  min-height: 220px;
  transition: transform 0.45s;
  transform-style: preserve-3d;
}
.face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 1.5rem;
  text-align: center;
}
.verso {
  transform: rotateY(180deg);
}
.face-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}
.nav-acoes {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.resultado {
  max-width: 480px;
  margin: 0 auto 1rem;
  text-align: center;
}
.ok {
  color: var(--color-accent-600);
  font-weight: 600;
}
.ko {
  color: var(--color-danger);
  font-weight: 600;
}
.meta {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
</style>
