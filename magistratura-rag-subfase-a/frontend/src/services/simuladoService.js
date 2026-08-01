import http, { IA_TIMEOUT_MS } from '@/api/http'

export default {
  listar() {
    return http.get('/simulados').then((r) => r.data)
  },
  obter(id) {
    return http.get(`/simulados/${id}`).then((r) => r.data)
  },
  iniciar(id) {
    return http.post(`/simulados/${id}/iniciar`).then((r) => r.data)
  },
  responder(tentativaId, questaoId, resposta) {
    return http
      .post(`/simulados/tentativas/${tentativaId}/responder`, { questaoId, resposta })
      .then((r) => r.data)
  },
  finalizar(tentativaId) {
    return http.post(`/simulados/tentativas/${tentativaId}/finalizar`).then((r) => r.data)
  },
  historico() {
    return http.get('/simulados/historico').then((r) => r.data)
  },

  eliminar(id) {
    return http.delete(`/simulados/${id}`).then((r) => r.data)
  },

  /**
   * Gera um simulado via IA (Knowledge Layer).
   * Contrato: POST /api/simulados/gerar
   * Body: GerarSimuladoRequest — titulo obrigatório; diplomaId | artigoId | assunto (pelo menos um).
   */
  gerar({
    titulo,
    descricao,
    diplomaId,
    artigoId,
    assunto,
    dificuldade,
    quantidadeQuestoes,
    tempoMinutos
  }) {
    return http
      .post(
      '/simulados/gerar',
      {
        titulo,
        descricao: descricao || undefined,
        diplomaId: diplomaId || undefined,
        artigoId: artigoId || undefined,
        assunto: assunto || undefined,
        dificuldade: dificuldade || undefined,
        quantidadeQuestoes: quantidadeQuestoes != null ? Number(quantidadeQuestoes) : undefined,
        tempoMinutos: tempoMinutos != null ? Number(tempoMinutos) : undefined
      },
      { timeout: IA_TIMEOUT_MS }
    )
      .then((r) => r.data)
  }
}
