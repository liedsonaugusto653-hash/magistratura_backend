import { defineStore } from 'pinia'
import simuladoService from '@/services/simuladoService'

export const useSimuladoStore = defineStore('simulado', {
  state: () => ({
    lista: [],
    tentativa: null,
    questoes: [],
    resultado: null,
    historico: [],
    carregando: false,
    erro: null,
    // Geração via IA — separado do loading da listagem
    aGerar: false,
    erroGeracao: null,
    sucessoGeracao: null,
    ultimoGerado: null
  }),
  actions: {
    async carregar() {
      this.carregando = true
      this.erro = null
      try {
        this.lista = await simuladoService.listar()
      } catch (e) {
        this.erro = e.response?.data?.mensagem || e.response?.data?.message || 'Erro ao carregar simulados'
      } finally {
        this.carregando = false
      }
    },

    async gerar(dados) {
      this.aGerar = true
      this.erroGeracao = null
      this.sucessoGeracao = null
      this.ultimoGerado = null
      try {
        const criado = await simuladoService.gerar(dados)
        this.ultimoGerado = criado
        this.sucessoGeracao =
          `Simulado «${criado.titulo}» criado com sucesso — ${criado.quantidadeQuestoes} questão(ões), ${criado.tempoMinutos} min.`
        // Atualiza lista sem misturar flags de loading
        try {
          this.lista = await simuladoService.listar()
        } catch {
          // lista pode falhar sem invalidar a geração
        }
        return criado
      } catch (e) {
        const d = e.response?.data
        this.erroGeracao =
          (typeof d === 'string' ? d : null) ||
          d?.mensagem ||
          d?.message ||
          d?.detail ||
          d?.erro ||
          (e.code === 'ECONNABORTED'
            ? 'O pedido demorou demasiado (timeout). Com Ollama local, tenta menos questões ou um modelo mais rápido.'
            : null) ||
          e.message ||
          'Não foi possível criar o simulado. Confirma que o diploma tem artigos processados e que a IA está a responder no Tutor.'
        throw e
      } finally {
        this.aGerar = false
      }
    },

    limparFeedbackGeracao() {
      this.erroGeracao = null
      this.sucessoGeracao = null
      this.ultimoGerado = null
    },

    async iniciar(id) {
      this.carregando = true
      this.resultado = null
      try {
        const data = await simuladoService.iniciar(id)
        this.tentativa = data
        this.questoes = data.questoes || []
      } finally {
        this.carregando = false
      }
    },
    async responder(questaoId, resposta) {
      if (!this.tentativa) return
      await simuladoService.responder(this.tentativa.tentativaId, questaoId, resposta)
    },
    async finalizar() {
      if (!this.tentativa) return
      this.resultado = await simuladoService.finalizar(this.tentativa.tentativaId)
      this.tentativa = null
    },
    async carregarHistorico() {
      this.historico = await simuladoService.historico()
    },
    async eliminar(id) {
      await simuladoService.eliminar(id)
      this.lista = this.lista.filter((s) => s.id !== id)
    }
  }
})
