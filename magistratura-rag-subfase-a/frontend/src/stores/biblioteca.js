import { defineStore } from 'pinia'
import bibliotecaService from '@/services/bibliotecaService'

export const useBibliotecaStore = defineStore('biblioteca', {
  state: () => ({
    diplomas: [],
    artigos: [],
    categorias: [],
    temas: [],
    diplomaAtual: null,
    artigoAtual: null,
    carregando: false,
    aGuardar: false,
    erro: null
  }),
  actions: {
    async carregarCategorias() {
      this.categorias = await bibliotecaService.listarCategorias()
    },
    async carregarTemas() {
      this.temas = await bibliotecaService.listarTemas()
    },
    async carregarDiplomas(params = {}) {
      this.carregando = true
      this.erro = null
      try {
        const data = await bibliotecaService.listarDiplomas(params)
        this.diplomas = data.content || data
      } catch (e) {
        this.erro = e.response?.data?.message || 'Erro ao carregar diplomas'
      } finally {
        this.carregando = false
      }
    },
    async obterDiploma(id) {
      this.carregando = true
      try {
        this.diplomaAtual = await bibliotecaService.obterDiploma(id)
      } finally {
        this.carregando = false
      }
    },
    async criarDiploma(dados) {
      this.aGuardar = true
      this.erro = null
      try {
        const criado = await bibliotecaService.criarDiploma(dados)
        this.diplomas.unshift(criado)
        return criado
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível criar o diploma.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async actualizarDiploma(id, dados) {
      this.aGuardar = true
      this.erro = null
      try {
        const actualizado = await bibliotecaService.actualizarDiploma(id, dados)
        const i = this.diplomas.findIndex((d) => d.id === id)
        if (i >= 0) this.diplomas[i] = { ...this.diplomas[i], ...actualizado }
        if (this.diplomaAtual?.id === id) this.diplomaAtual = { ...this.diplomaAtual, ...actualizado }
        return actualizado
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível actualizar o diploma.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async eliminarDiploma(id) {
      this.aGuardar = true
      this.erro = null
      try {
        await bibliotecaService.eliminarDiploma(id)
        this.diplomas = this.diplomas.filter((d) => d.id !== id)
        if (this.diplomaAtual?.id === id) this.diplomaAtual = null
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível eliminar o diploma.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async carregarArtigos(params = {}) {
      this.carregando = true
      try {
        const data = await bibliotecaService.listarArtigos(params)
        this.artigos = data.content || data
      } finally {
        this.carregando = false
      }
    },
    async obterArtigo(id) {
      this.artigoAtual = await bibliotecaService.obterArtigo(id)
    }
  }
})
