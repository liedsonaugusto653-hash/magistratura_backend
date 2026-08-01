import http from '@/api/http'

export default {
  login(email, password) {
    return http.post('/auth/login', { email, password }).then((r) => r.data)
  },

  registo(nome, email, password) {
    return http.post('/auth/registo', { nome, email, password }).then((r) => r.data)
  },

  recuperarPassword(email) {
    return http.post('/auth/recuperar-password', { email }).then((r) => r.data)
  },

  redefinirPassword(token, novaPassword) {
    return http.post('/auth/redefinir-password', { token, novaPassword }).then((r) => r.data)
  },

  logout() {
    return http.post('/auth/logout').catch(() => {})
  },

  me() {
    return http.get('/auth/me').then((r) => r.data)
  },

  /**
   * PATCH /auth/me — devolve LoginResponse { token, utilizador, ... }
   */
  atualizarPerfil({ nome, email, fotografiaUrl }) {
    return http.patch('/auth/me', { nome, email, fotografiaUrl }).then((r) => r.data)
  },

  alterarPassword({ passwordAtual, novaPassword }) {
    return http.post('/auth/me/password', { passwordAtual, novaPassword })
  },

  /**
   * PUT /auth/me/preferencias
   * @param {string} preferenciasJson JSON stringificado
   */
  atualizarPreferencias(preferenciasJson) {
    return http
      .put('/auth/me/preferencias', { preferenciasJson })
      .then((r) => r.data)
  }
}
