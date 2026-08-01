import { defineStore } from 'pinia'
import flashcardService from '@/services/flashcardService'

export const useFlashcardStore = defineStore('flashcard', {
  state: () => ({
    lista: [],
    atual: null,
    indice: 0,
    resultado: null,
    carregando: false,
    erro: null,
    aGuardar: false
  }),
  getters: {
    cartaoAtual: (s) => s.lista[s.indice] || null,
    total: (s) => s.lista.length
  },
  actions: {
    async carregar() {
      this.carregando = true
      this.erro = null
      try {
        this.lista = await flashcardService.listar()
        this.indice = 0
        this.resultado = null
      } catch (e) {
        this.erro = e.response?.data?.message || 'Erro ao carregar flashcards'
      } finally {
        this.carregando = false
      }
    },
    async revisar(acertou) {
      const cartao = this.cartaoAtual
      if (!cartao) return
      this.resultado = await flashcardService.revisar(cartao.id, acertou)
      cartao.progresso = {
        vezesRevisto: this.resultado.vezesRevisto,
        acertos: this.resultado.acertos,
        erros: this.resultado.erros,
        percentagemAcerto: this.resultado.percentagemAcerto,
        nivelDificuldade: this.resultado.nivelDificuldade
      }
    },
    seguinte() {
      this.resultado = null
      if (this.indice < this.lista.length - 1) this.indice++
    },
    anterior() {
      this.resultado = null
      if (this.indice > 0) this.indice--
    },
    async criar(dados) {
      this.aGuardar = true
      this.erro = null
      try {
        const criado = await flashcardService.criar(dados)
        this.lista.push(criado)
        return criado
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível criar o flashcard.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async actualizar(id, dados) {
      this.aGuardar = true
      this.erro = null
      try {
        const actualizado = await flashcardService.actualizar(id, dados)
        const i = this.lista.findIndex((f) => f.id === id)
        if (i >= 0) this.lista[i] = { ...this.lista[i], ...actualizado }
        return actualizado
      } catch (e) {
        this.erro =
          e.response?.data?.mensagem || e.response?.data?.message || 'Não foi possível actualizar o flashcard.'
        throw e
      } finally {
        this.aGuardar = false
      }
    },
    async eliminar(id) {
      const target = id || this.cartaoAtual?.id
      if (!target) return
      await flashcardService.eliminar(target)
      this.lista = this.lista.filter((f) => f.id !== target)
      if (this.indice >= this.lista.length) {
        this.indice = Math.max(0, this.lista.length - 1)
      }
      this.resultado = null
    }
  }
})
