<script setup>
import PageHint from '@/components/ui/PageHint.vue'
import { confirmarEliminacao } from '@/utils/prefsUi'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuestaoStore } from '@/stores/questao'
import bibliotecaService from '@/services/bibliotecaService'
import { ListChecks, Plus, Pencil } from 'lucide-vue-next'
import { EmptyState, LoadingState, BaseBadge, BaseButton, BaseCard, BaseSelect } from '@/components/ui'
import { PageHero } from '@/components/ui'

const store = useQuestaoStore()
const router = useRouter()
const questaoAtiva = ref(null)
const escolha = ref(null)
const modo = ref('lista') // lista | detalhe | formulario
const editandoId = ref(null)
const formErro = ref('')
const diplomas = ref([])

const form = reactive({
  enunciado: '',
  opcaoA: '',
  opcaoB: '',
  opcaoC: '',
  opcaoD: '',
  respostaCorreta: 'A',
  justificacao: '',
  nivelDificuldade: 'MEDIO',
  diplomaId: ''
})

const opcoesResposta = [
  { value: 'A', label: 'A' },
  { value: 'B', label: 'B' },
  { value: 'C', label: 'C' },
  { value: 'D', label: 'D' }
]
const opcoesDificuldade = [
  { value: 'FACIL', label: 'Fácil' },
  { value: 'MEDIO', label: 'Média' },
  { value: 'DIFICIL', label: 'Difícil' }
]

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

async function abrir(q) {
  await store.obter(q.id)
  questaoAtiva.value = store.atual
  escolha.value = null
  store.resultado = null
  modo.value = 'detalhe'
}

async function responder() {
  if (!escolha.value || !questaoAtiva.value) return
  await store.responder(questaoAtiva.value.id, escolha.value)
}

function voltar() {
  questaoAtiva.value = null
  store.resultado = null
  modo.value = 'lista'
}

function irGerar() {
  router.push({ name: 'ferramentas', query: { aba: 'questoes' } })
}

function abrirNovo() {
  editandoId.value = null
  form.enunciado = ''
  form.opcaoA = ''
  form.opcaoB = ''
  form.opcaoC = ''
  form.opcaoD = ''
  form.respostaCorreta = 'A'
  form.justificacao = ''
  form.nivelDificuldade = 'MEDIO'
  form.diplomaId = ''
  formErro.value = ''
  modo.value = 'formulario'
}

async function abrirEditar(id, ev) {
  if (ev) ev.stopPropagation()
  formErro.value = ''
  try {
    const q = await store.obterCompleto(id)
    editandoId.value = q.id
    form.enunciado = q.enunciado || ''
    form.opcaoA = q.opcaoA || ''
    form.opcaoB = q.opcaoB || ''
    form.opcaoC = q.opcaoC || ''
    form.opcaoD = q.opcaoD || ''
    form.respostaCorreta = q.respostaCorreta || 'A'
    form.justificacao = q.justificacao || ''
    form.nivelDificuldade = q.nivelDificuldade || 'MEDIO'
    form.diplomaId = q.diplomaId || ''
    modo.value = 'formulario'
  } catch {
    formErro.value = 'Não foi possível carregar a questão para edição.'
  }
}

function cancelarForm() {
  modo.value = questaoAtiva.value ? 'detalhe' : 'lista'
  formErro.value = ''
}

async function guardarForm() {
  formErro.value = ''
  if (!form.enunciado.trim()) {
    formErro.value = 'Indica o enunciado.'
    return
  }
  for (const letra of ['A', 'B', 'C', 'D']) {
    if (!form['opcao' + letra]?.trim()) {
      formErro.value = `Preenche a opção ${letra}.`
      return
    }
  }
  const payload = {
    enunciado: form.enunciado.trim(),
    opcaoA: form.opcaoA.trim(),
    opcaoB: form.opcaoB.trim(),
    opcaoC: form.opcaoC.trim(),
    opcaoD: form.opcaoD.trim(),
    respostaCorreta: form.respostaCorreta,
    justificacao: form.justificacao.trim() || undefined,
    nivelDificuldade: form.nivelDificuldade || 'MEDIO',
    diplomaId: form.diplomaId || undefined
  }
  try {
    if (editandoId.value) {
      await store.actualizar(editandoId.value, payload)
    } else {
      await store.criar(payload)
    }
    questaoAtiva.value = null
    modo.value = 'lista'
  } catch (_) {
    formErro.value = store.erro || 'Não foi possível guardar.'
  }
}

async function eliminarQuestao(id, ev) {
  if (ev) ev.stopPropagation()
  if (!confirmarEliminacao('Eliminar esta questão?')) return
  try {
    await store.eliminar(id)
    if (questaoAtiva.value?.id === id) voltar()
  } catch (_) {}
}
</script>

<template>
  <div class="page">
    <PageHero
      eyebrow="Questões"
      title="Banco de Questões"
      lead="Treina com questões alinhadas ao programa de preparação."
      art="questoes"
    />
    <PageHint text="Pratica com questões de escolha múltipla — manuais ou geradas com IA a partir da legislação." />


    <div v-if="modo !== 'formulario'" class="acoes-topo">
      <BaseButton variant="secondary" @click="abrirNovo">
        <Plus :size="16" /> Nova questão
      </BaseButton>
      <BaseButton variant="ghost" @click="irGerar">
        <ListChecks :size="16" /> Gerar com IA
      </BaseButton>
    </div>

    <!-- Formulário -->
    <form v-if="modo === 'formulario'" class="card form-manual" @submit.prevent="guardarForm">
      <h2>{{ editandoId ? 'Editar questão' : 'Nova questão' }}</h2>
      <div class="field">
        <label>Enunciado *</label>
        <textarea v-model="form.enunciado" rows="3" required maxlength="4000" />
      </div>
      <div v-for="letra in ['A', 'B', 'C', 'D']" :key="letra" class="field">
        <label>Opção {{ letra }} *</label>
        <input v-model="form['opcao' + letra]" type="text" required maxlength="2000" />
      </div>
      <div class="field-row">
        <div class="field">
          <label>Resposta correcta *</label>
          <BaseSelect v-model="form.respostaCorreta" :options="opcoesResposta" />
        </div>
        <div class="field">
          <label>Dificuldade</label>
          <BaseSelect v-model="form.nivelDificuldade" :options="opcoesDificuldade" />
        </div>
      </div>
      <div class="field">
        <label>Justificação (opcional)</label>
        <textarea v-model="form.justificacao" rows="2" maxlength="4000" />
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
      <LoadingState v-if="store.carregando" message="A carregar questões…" />

      <template v-else-if="modo === 'detalhe' && questaoAtiva">
        <BaseButton variant="ghost" style="margin-bottom: 1rem" @click="voltar">← Voltar</BaseButton>
        <BaseCard static-hover style="max-width: 640px; margin: 0 auto">
          <div style="display: flex; justify-content: flex-end; gap: 0.4rem; margin-bottom: 0.5rem">
            <button type="button" class="btn btn-ghost" style="font-size: 0.78rem" @click="abrirEditar(questaoAtiva.id)">
              <Pencil :size="12" /> Editar
            </button>
            <button type="button" class="btn btn-ghost" style="font-size: 0.78rem" @click="eliminarQuestao(questaoAtiva.id)">
              Eliminar
            </button>
          </div>
          <p style="font-weight: 600; margin-bottom: 1rem">{{ questaoAtiva.enunciado }}</p>
          <div style="display: flex; flex-direction: column; gap: 0.6rem">
            <button
              v-for="letra in ['A', 'B', 'C', 'D']"
              :key="letra"
              class="btn btn-ghost"
              style="justify-content: flex-start; text-align: left"
              :class="{
                'btn-secondary': escolha === letra && !store.resultado,
                'btn-accent': store.resultado && store.resultado.respostaCorreta === letra
              }"
              :style="
                store.resultado && escolha === letra && !store.resultado.correta
                  ? 'border-color:var(--color-danger);color:var(--color-danger)'
                  : ''
              "
              :disabled="!!store.resultado"
              @click="escolha = letra"
            >
              <strong>{{ letra }})</strong>&nbsp;
              {{ questaoAtiva['opcao' + letra] }}
            </button>
          </div>

          <BaseButton
            v-if="!store.resultado"
            variant="primary"
            block
            style="margin-top: 1.25rem"
            :disabled="!escolha"
            @click="responder"
          >
            Confirmar resposta
          </BaseButton>

          <div
            v-if="store.resultado"
            style="margin-top: 1.25rem; padding: 1rem; border-radius: var(--radius-sm)"
            :style="{
              background: store.resultado.correta ? 'var(--color-accent-100)' : '#fbeae6'
            }"
          >
            <strong>{{ store.resultado.correta ? 'Correto!' : 'Incorreto' }}</strong>
            <p style="margin: 0.4rem 0 0; font-size: 0.88rem">
              Resposta correcta: <strong>{{ store.resultado.respostaCorreta }}</strong>
            </p>
            <p
              v-if="store.resultado.justificacao"
              style="margin: 0.4rem 0 0; font-size: 0.85rem; color: var(--color-text-soft)"
            >
              {{ store.resultado.justificacao }}
            </p>
          </div>
        </BaseCard>
      </template>

      <EmptyState
        v-else-if="!store.lista.length"
        title="Ainda não existem questões"
        description="Cria uma questão manualmente ou gera a partir da legislação com IA."
        :icon="ListChecks"
      >
        <BaseButton variant="primary" @click="abrirNovo">
          <Plus :size="16" /> Criar questão
        </BaseButton>
        <BaseButton variant="secondary" @click="irGerar">
          <ListChecks :size="16" /> Gerar com IA
        </BaseButton>
      </EmptyState>

      <div v-else class="grid grid-2">
        <BaseCard v-for="q in store.lista" :key="q.id" interactive @click="abrir(q)">
          <p style="margin: 0; font-size: 0.92rem; font-weight: 500">
            {{ q.enunciado?.slice(0, 120) }}{{ q.enunciado?.length > 120 ? '…' : '' }}
          </p>
          <div
            style="
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-top: 0.6rem;
              gap: 0.5rem;
            "
          >
            <BaseBadge variant="soon" :label="q.nivelDificuldade || 'MEDIO'" />
            <div style="display: flex; gap: 0.25rem">
              <button
                type="button"
                class="btn btn-ghost"
                style="font-size: 0.75rem; padding: 0.25rem 0.5rem"
                @click="abrirEditar(q.id, $event)"
              >
                Editar
              </button>
              <button
                type="button"
                class="btn btn-ghost"
                style="font-size: 0.75rem; padding: 0.25rem 0.5rem"
                @click="eliminarQuestao(q.id, $event)"
              >
                Eliminar
              </button>
            </div>
          </div>
        </BaseCard>
      </div>
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
  max-width: 640px;
  margin: 0 auto 1.5rem;
  padding: 1.35rem 1.5rem;
}
.form-manual h2 {
  margin: 0 0 1rem;
  font-size: 1.05rem;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
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
@media (max-width: 560px) {
  .field-row {
    grid-template-columns: 1fr;
  }
}
</style>
