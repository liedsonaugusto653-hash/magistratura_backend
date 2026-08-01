import http from '@/api/http'

export default {
  listar() {
    return http.get('/flashcards').then((r) => r.data)
  },
  obter(id) {
    return http.get(`/flashcards/${id}`).then((r) => r.data)
  },
  criar(dados) {
    return http.post('/flashcards', dados).then((r) => r.data)
  },
  actualizar(id, dados) {
    return http.put(`/flashcards/${id}`, dados).then((r) => r.data)
  },
  revisar(id, acertou) {
    return http.post(`/flashcards/${id}/revisar`, { acertou }).then((r) => r.data)
  },
  eliminar(id) {
    return http.delete(`/flashcards/${id}`).then((r) => r.data)
  }
}
