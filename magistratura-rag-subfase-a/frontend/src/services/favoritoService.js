import http from '@/api/http'

export default {
  listar() {
    return http.get('/favoritos').then((r) => r.data)
  },
  estadoArtigo(artigoId) {
    return http.get(`/favoritos/artigos/${artigoId}/estado`).then((r) => r.data)
  },
  adicionarArtigo(artigoId) {
    return http.post('/favoritos', { artigoId }).then((r) => r.data)
  },
  adicionarDiploma(diplomaId) {
    return http.post('/favoritos', { diplomaId }).then((r) => r.data)
  },
  removerArtigo(artigoId) {
    return http.delete(`/favoritos/artigos/${artigoId}`)
  },
  remover(id) {
    return http.delete(`/favoritos/${id}`)
  }
}
