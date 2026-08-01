import http, { IA_TIMEOUT_MS, getAuthToken, apiUrl } from '@/api/http'

/** Timeout de inatividade do stream SSE (modelos locais podem demorar 1–5 min). */
const STREAM_IDLE_MS = IA_TIMEOUT_MS || 320_000

export default {
  status() {
    return http.get('/ia/status').then((r) => r.data)
  },

  listarConversas() {
    return http.get('/ia/conversas').then((r) => r.data)
  },
  criarConversa(titulo) {
    return http.post('/ia/conversas', titulo ? { titulo } : {}).then((r) => r.data)
  },
  obterConversa(id) {
    return http.get(`/ia/conversas/${id}`).then((r) => r.data)
  },
  eliminarConversa(id) {
    return http.delete(`/ia/conversas/${id}`)
  },

  chat({ conversaId, mensagem, diplomaId, artigoId, trecho, topicoId }) {
    return http
      .post(
        '/ia/chat',
        { conversaId, mensagem, diplomaId, artigoId, trecho, topicoId },
        { timeout: STREAM_IDLE_MS }
      )
      .then((r) => r.data)
  },

  /**
   * Streaming SSE.
   * Resolve assim que chega `event: concluido` — não espera o fecho da ligação
   * (heartbeats podiam manter o reader aberto e bloquear aEnviar para sempre).
   */
  async chatStream(
    { conversaId, mensagem, diplomaId, artigoId, trecho, topicoId },
    onChunk,
    onFontes,
    onConversa
  ) {
    const token = getAuthToken()

    const controlador = new AbortController()
    let temporizador = setTimeout(() => controlador.abort(), STREAM_IDLE_MS)
    const reiniciarTemporizador = () => {
      clearTimeout(temporizador)
      temporizador = setTimeout(() => controlador.abort(), STREAM_IDLE_MS)
    }

    let response
    try {
      response = await fetch(apiUrl('/ia/chat/stream'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token ? `Bearer ${token}` : '',
          Accept: 'text/event-stream'
        },
        body: JSON.stringify({ conversaId, mensagem, diplomaId, artigoId, trecho, topicoId }),
        signal: controlador.signal
      })
    } catch (e) {
      clearTimeout(temporizador)
      if (e.name === 'AbortError') {
        throw new Error('O Tutor IA demorou demasiado tempo a responder. Tenta de novo.')
      }
      throw e
    }

    if (!response.ok || !response.body) {
      clearTimeout(temporizador)
      throw new Error('Não foi possível iniciar o streaming do Tutor IA')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let recebeuAlgumToken = false
    let concluido = false

    try {
      while (!concluido) {
        let done, value
        try {
          ;({ done, value } = await reader.read())
        } catch (e) {
          if (e.name === 'AbortError') {
            throw new Error('O Tutor IA demorou demasiado tempo a responder. Tenta de novo.')
          }
          throw e
        }
        if (done) break

        reiniciarTemporizador()
        buffer += decoder.decode(value, { stream: true })

        const partes = buffer.split('\n\n')
        buffer = partes.pop()

        for (const parte of partes) {
          if (!parte.trim()) continue

          let evento = 'message'
          let dados = ''

          for (const linha of parte.split('\n')) {
            if (linha.startsWith('event:')) {
              evento = linha.slice(6).trim()
            } else if (linha.startsWith('data:')) {
              dados += (dados ? '\n' : '') + linha.slice(5).replace(/^ /, '')
            }
          }

          if (evento === 'heartbeat') continue

          if (evento === 'conversa' && dados) {
            try {
              const obj = JSON.parse(dados)
              if (obj?.id && typeof onConversa === 'function') onConversa(obj.id)
            } catch {
              /* ignore */
            }
            continue
          }

          if (evento === 'fontes' && dados) {
            try {
              const lista = JSON.parse(dados)
              if (Array.isArray(lista) && typeof onFontes === 'function') onFontes(lista)
            } catch {
              /* ignore */
            }
            continue
          }

          if (evento === 'erro') {
            let mensagemErro = dados
            try {
              mensagemErro = JSON.parse(dados)
            } catch {
              /* bruto */
            }
            throw new Error(mensagemErro || 'O Tutor IA reportou um erro durante a resposta.')
          }

          if (evento === 'concluido') {
            concluido = true
            break
          }

          if (evento === 'token' && dados !== '') {
            let texto = dados
            try {
              texto = JSON.parse(dados)
            } catch {
              /* bruto */
            }
            if (texto !== '' && texto !== 'true' && texto !== 'false') {
              recebeuAlgumToken = true
              onChunk(texto)
            }
          }
        }
      }
    } finally {
      clearTimeout(temporizador)
      try {
        await reader.cancel()
      } catch {
        /* ignore */
      }
      try {
        controlador.abort()
      } catch {
        /* ignore */
      }
    }

    if (!recebeuAlgumToken) {
      throw new Error('O streaming terminou sem qualquer conteúdo do Tutor IA.')
    }
  },

  resumir({ diplomaId, artigoId, texto }) {
    return http.post('/ia/resumo', { diplomaId, artigoId, texto }, { timeout: STREAM_IDLE_MS }).then((r) => r.data)
  },
  explicar({ artigoId, trecho }) {
    return http.post('/ia/explicar', { artigoId, trecho }, { timeout: STREAM_IDLE_MS }).then((r) => r.data)
  },
  gerarFlashcards({ diplomaId, artigoId, quantidade, guardar }) {
    const q = Math.min(Math.max(Number(quantidade) || 3, 1), 5)
    return http
      .post('/ia/flashcards', { diplomaId, artigoId, quantidade: q, guardar }, { timeout: STREAM_IDLE_MS })
      .then((r) => r.data)
  },
  gerarQuestoes({ diplomaId, artigoId, quantidade, guardar }) {
    const q = Math.min(Math.max(Number(quantidade) || 2, 1), 3)
    return http
      .post('/ia/questoes', { diplomaId, artigoId, quantidade: q, guardar }, { timeout: STREAM_IDLE_MS })
      .then((r) => r.data)
  }
}
