import { defineStore } from 'pinia'
import estatisticaService from '@/services/estatisticaService'

/** Fonte unica de progresso no frontend (Dashboard + Estatisticas). */
export const useEstatisticaStore = defineStore('estatistica', {
  state: () => ({
    dados: null,
    carregando: false,
    erro: null,
    carregadoEm: null
  }),
  getters: {
    horasEstudo: (s) => s.dados?.horasEstudo ?? 0,
    questoesRespondidas: (s) => s.dados?.questoesRespondidas ?? 0,
    flashcardsConcluidos: (s) => s.dados?.flashcardsConcluidos ?? 0,
    percentagemSucessoQuestoes: (s) => s.dados?.percentagemSucessoQuestoes ?? 0,
    percentagemSucessoFlashcards: (s) => s.dados?.percentagemSucessoFlashcards ?? 0,
    diasConsecutivos: (s) => s.dados?.diasConsecutivos ?? 0
  },
  actions: {
    async carregar({ forcar = false } = {}) {
      if (
        !forcar &&
        this.dados &&
        this.carregadoEm &&
        Date.now() - this.carregadoEm < 15_000
      ) {
        return this.dados
      }
      this.carregando = true
      this.erro = null
      try {
        this.dados = await estatisticaService.obter()
        this.carregadoEm = Date.now()
        return this.dados
      } catch (e) {
        this.erro =
          e.response?.data?.message ||
          e.response?.data?.mensagem ||
          'Erro ao carregar estatisticas'
        throw e
      } finally {
        this.carregando = false
      }
    },
    invalidar() {
      this.carregadoEm = null
    }
  }
})
