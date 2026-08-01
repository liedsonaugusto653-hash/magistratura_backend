import http from '@/api/http'

export default {
  listar(params = {}) {
    return http.get('/questoes', { params }).then((r) => r.data)
  },
  obter(id) {
    return http.get(`/questoes/${id}`).then((r) => r.data)
  },
  obterCompleto(id) {
    return http.get(`/questoes/${id}/completo`).then((r) => r.data)
  },
  criar(dados) {
    return http.post('/questoes', dados).then((r) => r.data)
  },
  actualizar(id, dados) {
    return http.put(`/questoes/${id}`, dados).then((r) => r.data)
  },
  responder(id, resposta) {
    return http.post(`/questoes/${id}/responder`, { resposta }).then((r) => r.data)
  },
  eliminar(id) {
    return http.delete(`/questoes/${id}`).then((r) => r.data)
  }
}
