import http, { IA_TIMEOUT_MS } from '@/api/http'

export default {
  listarEntidades() {
    return http.get('/ontologia/entidades').then((r) => r.data)
  },
  obterEntidade(id) {
    return http.get(`/ontologia/entidades/${id}`).then((r) => r.data)
  },
  obterPorCodigo(codigo) {
    return http.get(`/ontologia/entidades/codigo/${encodeURIComponent(codigo)}`).then((r) => r.data)
  },
  mapa(entidadeId) {
    return http.get(`/ontologia/entidades/${entidadeId}/mapa`).then((r) => r.data)
  },
  trilha(entidadeId) {
    return http.get(`/ontologia/entidades/${entidadeId}/trilha`).then((r) => r.data)
  },
  topicosPorEntidade(entidadeId) {
    return http.get(`/ontologia/entidades/${entidadeId}/topicos`).then((r) => r.data)
  },
  pesquisarTopicos(termo) {
    return http.get('/ontologia/topicos', { params: { termo } }).then((r) => r.data)
  },
  obterTopico(id) {
    return http.get(`/ontologia/topicos/${id}`).then((r) => r.data)
  },
  artigosDoTopico(topicoId) {
    return http.get(`/ontologia/topicos/${topicoId}/artigos`).then((r) => r.data)
  },
  ligarArtigo(topicoId, artigoId, relevancia = 1.0) {
    return http
      .post(`/ontologia/topicos/${topicoId}/artigos`, {
        artigoId,
        relevancia,
        origemLigacao: 'MANUAL'
      })
      .then((r) => r.data)
  },
  desligarArtigo(topicoId, artigoId) {
    return http.delete(`/ontologia/topicos/${topicoId}/artigos/${artigoId}`)
  },
  gerarFichaEstudo(topicoId, forcar = false) {
    return http
      .post(`/ontologia/topicos/${topicoId}/ficha-estudo`, { forcar }, { timeout: IA_TIMEOUT_MS })
      .then((r) => r.data)
  }
}
