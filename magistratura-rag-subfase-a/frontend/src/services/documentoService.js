import http from '@/api/http'

export default {
  /**
   * Importa um novo PDF para a Biblioteca Jurídica.
   * Endpoint: POST /api/documentos (multipart/form-data)
   *
   * @param {Object} dados
   * @param {File}   dados.ficheiro       - o ficheiro PDF (obrigatório)
   * @param {string} dados.titulo         - título do documento (obrigatório)
   * @param {string} [dados.categoriaId]  - UUID da categoria
   * @param {string} [dados.fonte]        - fonte do documento
   * @param {boolean} [dados.oficial]     - se é fonte oficial (default true)
   * @param {string} [dados.dataPublicacao] - data ISO (yyyy-MM-dd)
   */
  importar(dados) {
    const formData = new FormData()
    formData.append('ficheiro', dados.ficheiro)
    formData.append('titulo', dados.titulo)
    if (dados.categoriaId) formData.append('categoriaId', dados.categoriaId)
    if (dados.fonte) formData.append('fonte', dados.fonte)
    formData.append('oficial', dados.oficial ?? true)
    if (dados.dataPublicacao) formData.append('dataPublicacao', dados.dataPublicacao)

    // O cliente axios partilhado (src/api/http.js) define 'Content-Type: application/json'
    // por omissão para todos os pedidos. Isso impede o axios de gerar automaticamente o
    // cabeçalho 'multipart/form-data; boundary=...' quando o corpo é um FormData. Por isso,
    // anulamos explicitamente o Content-Type aqui, para o browser o definir sozinho, com o
    // boundary correto.
    return http.post('/documentos', formData, {
      headers: { 'Content-Type': undefined }
    }).then((r) => r.data)
  },

  /**
   * Extrai o texto do PDF e estrutura-o em artigos, associados a um diploma
   * já existente.
   * Endpoint: POST /api/documentos/{id}/processar
   */
  processar(id, diplomaId) {
    return http.post(`/documentos/${id}/processar`, null, { params: { diplomaId }, timeout: 60000 }).then((r) => r.data)
  },

  listar(params = {}) {
    return http.get('/documentos', { params }).then((r) => r.data)
  },

  obter(id) {
    return http.get(`/documentos/${id}`).then((r) => r.data)
  },

  /** URL direto para o PDF — não usar em <a href> diretamente: exige o token JWT,
   *  que só é enviado pelo interceptor do axios, não por um link simples do browser.
   *  Mantido apenas para referência/depuração. */
  urlPdf(id) {
    return `/api/documentos/${id}/pdf`
  },

  /**
   * Obtém o PDF como Blob, já autenticado (o interceptor do http.js injeta o
   * Bearer token). Usar com URL.createObjectURL(blob) para abrir numa nova aba.
   */
  async obterPdfBlob(id) {
    const response = await http.get(`/documentos/${id}/pdf`, { responseType: 'blob' })
    return new Blob([response.data], { type: 'application/pdf' })
  },

  obterIndice(id) {
    return http.get(`/documentos/${id}/indice`).then((r) => r.data)
  },

  /**
   * Atualiza metadados de um documento (título, categoria, fonte, oficial,
   * dataPublicacao). Só envia os campos que forem passados.
   * Endpoint: PUT /api/documentos/{id}
   */
  atualizar(id, dados) {
    return http.put(`/documentos/${id}`, dados).then((r) => r.data)
  },

  /**
   * Elimina definitivamente um documento (artigos, embeddings e ficheiro
   * PDF associados são removidos no backend).
   * Endpoint: DELETE /api/documentos/{id}
   */
  eliminar(id, forcar = false) {
    return http.delete(`/documentos/${id}`, { params: { forcar } }).then((r) => r.data)
  }
}