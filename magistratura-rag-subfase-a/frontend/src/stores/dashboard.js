import { defineStore } from 'pinia'
import dashboardService from '@/services/dashboardService'
import { useEstatisticaStore } from '@/stores/estatistica'

/**
 * Metricas: sempre do store de estatisticas (GET /api/estatisticas).
 * /api/dashboard so acrescenta historico recente.
 */
export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    dados: null,
    carregando: false,
    erro: null
  }),
  getters: {
    resumo() {
      const est = useEstatisticaStore()
      const e = est.dados || {}
      const d = this.dados || {}
      const percQ =
        e.percentagemSucessoQuestoes ?? d.percentagemSucessoQuestoes ?? d.percentagemSucesso ?? 0
      return {
        nome: e.nome || d.nome,
        horasEstudo: e.horasEstudo ?? d.horasEstudo ?? 0,
        diasConsecutivos: e.diasConsecutivos ?? d.diasConsecutivos ?? 0,
        questoesRespondidas: e.questoesRespondidas ?? d.questoesRespondidas ?? 0,
        questoesCorretas: e.questoesCorretas ?? d.questoesCorretas ?? 0,
        percentagemSucessoQuestoes: percQ,
        percentagemSucesso: percQ,
        flashcardsConcluidos: e.flashcardsConcluidos ?? d.flashcardsConcluidos ?? 0,
        percentagemSucessoFlashcards:
          e.percentagemSucessoFlashcards ?? d.percentagemSucessoFlashcards ?? 0,
        simuladosRealizados: e.simuladosRealizados ?? d.simuladosRealizados ?? 0,
        ultimaAtividade: e.ultimaAtividade ?? d.ultimaAtividade ?? null,
        historicoRecente: d.historicoRecente || []
      }
    }
  },
  actions: {
    async carregar() {
      this.carregando = true
      this.erro = null
      const est = useEstatisticaStore()
      try {
        const [, dash] = await Promise.all([
          est.carregar(),
          dashboardService.obter().catch(() => null)
        ])
        this.dados = dash
      } catch (e) {
        this.erro =
          e.response?.data?.message ||
          e.response?.data?.mensagem ||
          est.erro ||
          'Erro ao carregar dashboard'
      } finally {
        this.carregando = false
      }
    }
  }
})
