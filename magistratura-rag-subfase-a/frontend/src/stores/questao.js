import { defineStore } from 'pinia'
import questaoService from '@/services/questaoService'
import { emitGuideEvent, GuideEvent } from '@/guide/events'
import { useEstatisticaStore } from '@/stores/estatistica'

export const useQuestaoStore = defineStore('questao', {
  state: () => ({
    lista: [],
    atual: null,
    resultado: null,
    carregando: false,
    erro: null,
    aGuardar: false
  }),
  actions: {
    async carregar(params = {}) {
      this.carregando = true
      this.erro = null
      this.resultado = null
      try {
        const data = await questaoService.listar(params)
        this.lista = data.content || data
      } catch (e) {
        this.erro = e.response?.data?.message || 'Erro ao carregar questões'
      } finally {
        this.carregando = false
      }
    },
    async obter(id) {
      this.atual = await questaoService.obter(id)
      this.resultado = null
    },
    async obterCompleto(id) {
      return questaoService.obterCompleto(id)
    },
    async responder(id, resposta) {
      this.resultado = await questaoService.responder(id, resposta)
      try {
        const correta = !!this.resultado?.correta
        emitGuideEvent(GuideEvent.QUESTION_ANSWERED, {
          correta,
          questaoId: id
        })
        if (!correta) {
          try {
            const pre = {
              mensagem:
                'Explica porque a resposta a esta questão pode não ser a que escolhi e indica o fundamento legal.',
              questaoId: id
            }
            sessionStorage.setItem('tutor_prefill', JSON.stringify(pre))
          } catch { /* ignore */ }
        }
      } catch {
        /* guide is best-effort */
      }
      try {
        useEstatisticaStore().invalidar()
      } catch {
        /* ignore */
      }
    },
    async criar(dados) {
      this.aGuardar = true
      this.erro = null
      try {
        const criada = await questaoService.criar(dados)
        this.lista.unshift(criada)
        return criada
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível criar a questão.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async actualizar(id, dados) {
      this.aGuardar = true
      this.erro = null
      try {
        const actualizada = await questaoService.actualizar(id, dados)
        const i = this.lista.findIndex((q) => q.id === id)
        if (i >= 0) this.lista[i] = { ...this.lista[i], ...actualizada }
        if (this.atual?.id === id) this.atual = { ...this.atual, ...actualizada }
        return actualizada
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível actualizar a questão.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async eliminar(id) {
      await questaoService.eliminar(id)
      this.lista = this.lista.filter((q) => q.id !== id)
      if (this.atual?.id === id) {
        this.atual = null
        this.resultado = null
      }
    }
  }
})
