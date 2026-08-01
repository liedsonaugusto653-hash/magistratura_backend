import http from '@/api/http'

export default {
  listarDiplomas(params = {}) {
    return http.get('/diplomas', { params: { size: 200, ...params } }).then((r) => r.data)
  },
  obterDiploma(id) {
    return http.get(`/diplomas/${id}`).then((r) => r.data)
  },
  criarDiploma(dados) {
    return http.post('/diplomas', dados).then((r) => r.data)
  },
  actualizarDiploma(id, dados) {
    return http.put(`/diplomas/${id}`, dados).then((r) => r.data)
  },
  eliminarDiploma(id) {
    return http.delete(`/diplomas/${id}`).then((r) => r.data)
  },
  listarArtigos(params = {}) {
    return http.get('/artigos', { params: { size: 300, ...params } }).then((r) => r.data)
  },
  obterArtigo(id) {
    return http.get(`/artigos/${id}`).then((r) => r.data)
  },
  listarCategorias() {
    return http.get('/categorias').then((r) => r.data)
  },
  obterCategoria(id) {
    return http.get(`/categorias/${id}`).then((r) => r.data)
  },
  listarTemas() {
    return http.get('/temas').then((r) => r.data)
  },
  obterTema(id) {
    return http.get(`/temas/${id}`).then((r) => r.data)
  }
}
