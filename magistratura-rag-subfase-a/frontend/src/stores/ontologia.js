import { defineStore } from 'pinia'
import ontologiaService from '@/services/ontologiaService'

export const useOntologiaStore = defineStore('ontologia', {
  state: () => ({
    entidades: [],
    mapa: null,
    trilha: [],
    topicoActivo: null,
    artigosTopico: [],
    carregando: false,
    carregandoMapa: false,
    aGerarFicha: false,
    erroFicha: null,
    erro: null
  }),

  actions: {
    async carregarEntidades() {
      this.carregando = true
      this.erro = null
      try {
        this.entidades = await ontologiaService.listarEntidades()
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem ||
          e.response?.data?.message ||
          'Não foi possível carregar o mapa jurídico.'
        this.entidades = []
      } finally {
        this.carregando = false
      }
    },

    async abrirMapa(entidadeId) {
      this.carregandoMapa = true
      this.erro = null
      this.topicoActivo = null
      this.artigosTopico = []
      try {
        const [mapa, trilha] = await Promise.all([
          ontologiaService.mapa(entidadeId),
          ontologiaService.trilha(entidadeId).catch(() => [])
        ])
        this.mapa = mapa
        this.trilha = trilha || []
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem ||
          e.response?.data?.message ||
          'Não foi possível carregar o mapa desta entidade.'
        this.mapa = null
        this.trilha = []
      } finally {
        this.carregandoMapa = false
      }
    },

    async seleccionarTopico(topicoId) {
      this.topicoActivo = null
      this.artigosTopico = []
      try {
        const [topico, artigos] = await Promise.all([
          ontologiaService.obterTopico(topicoId),
          ontologiaService.artigosDoTopico(topicoId)
        ])
        this.topicoActivo = topico
        this.artigosTopico = artigos || []
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem ||
          e.response?.data?.message ||
          'Não foi possível carregar o tópico.'
      }
    },

    limparMapa() {
      this.mapa = null
      this.trilha = []
      this.topicoActivo = null
      this.artigosTopico = []
    },

    async gerarFichaEstudo(topicoId, forcar = false) {
      this.aGerarFicha = true
      this.erroFicha = null
      try {
        const topicoActualizado = await ontologiaService.gerarFichaEstudo(topicoId, forcar)
        if (this.topicoActivo?.id === topicoId) {
          this.topicoActivo = topicoActualizado
        }
        return topicoActualizado
      } catch (e) {
        this.erroFicha =
          e.response?.data?.mensagem ||
          e.response?.data?.message ||
          'Não foi possível gerar a ficha de estudo deste conceito.'
        throw e
      } finally {
        this.aGerarFicha = false
      }
    }
  }
})
