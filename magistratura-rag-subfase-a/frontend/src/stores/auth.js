import { defineStore } from 'pinia'
import authService from '@/services/authService'
import { getAuthToken, setAuthToken, clearAuthToken } from '@/api/http'
import { normalizarPrefs } from '@/utils/prefsUi'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getAuthToken() || null,
    utilizador: null,
    carregando: false,
    erro: null
  }),

  getters: {
    autenticado: (state) => !!state.token,
    nome: (state) => state.utilizador?.nome || state.utilizador?.fullName || ''
  },

  actions: {
    async login(email, password) {
      this.carregando = true
      this.erro = null
      try {
        const resposta = await authService.login(email, password)
        this.token = resposta.token
        this.utilizador = resposta.utilizador
        setAuthToken(resposta.token)
        return true
      } catch (err) {
        this.erro = mensagemErro(err, 'Credenciais inválidas. Verifica o email e a palavra-passe.')
        return false
      } finally {
        this.carregando = false
      }
    },

    async registar(nome, email, password) {
      this.carregando = true
      this.erro = null
      try {
        const resposta = await authService.registo(nome, email, password)
        if (resposta?.token) {
          this.token = resposta.token
          this.utilizador = resposta.utilizador
          setAuthToken(resposta.token)
        }
        return true
      } catch (err) {
        this.erro = mensagemErro(err, 'Não foi possível criar a conta.')
        return false
      } finally {
        this.carregando = false
      }
    },

    async logout() {
      try {
        await authService.logout()
      } finally {
        this.token = null
        this.utilizador = null
        clearAuthToken()
      }
    },

    async carregarUtilizadorAtual() {
      if (!this.token) return
      this.carregando = true
      try {
        this.utilizador = await authService.me()
      } catch {
        this.token = null
        this.utilizador = null
        clearAuthToken()
      } finally {
        this.carregando = false
      }
    },

    async atualizarPerfil(payload) {
      this.erro = null
      const resposta = await authService.atualizarPerfil(payload)
      if (resposta?.token) {
        this.token = resposta.token
        setAuthToken(resposta.token)
      }
      this.utilizador = resposta?.utilizador || resposta
      return this.utilizador
    },

    async alterarPassword(payload) {
      this.erro = null
      await authService.alterarPassword(payload)
    },

    /**
     * Persiste preferências no backend (preferencias_json) e actualiza o utilizador em memória.
     * @param {object} obj objecto de preferências (não string)
     */
    async guardarPreferencias(obj) {
      const normalizado = normalizarPrefs(obj || {})
      const preferenciasJson = JSON.stringify(normalizado)
      this.utilizador = await authService.atualizarPreferencias(preferenciasJson)
      return this.utilizador
    },

    preferencias() {
      try {
        const raw = this.utilizador?.preferenciasJson
        if (!raw) return {}
        return normalizarPrefs(typeof raw === 'string' ? JSON.parse(raw) : raw)
      } catch {
        return {}
      }
    }
  }
})

function mensagemErro(err, fallback) {
  const d = err?.response?.data
  if (!d) return fallback
  if (typeof d.mensagem === 'string') return d.mensagem
  if (typeof d.message === 'string') return d.message
  if (typeof d.error === 'string') return d.error
  if (d.details && typeof d.details === 'object') {
    const first = Object.values(d.details)[0]
    if (typeof first === 'string') return first
  }
  if (err.response?.status === 429) return 'Demasiados pedidos. Tenta novamente dentro de um minuto.'
  if (err.response?.status === 422) return 'Email já registado ou dados inválidos.'
  if (err.response?.status === 400) return 'Dados inválidos. Verifica os campos.'
  return fallback
}
