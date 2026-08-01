import { TOKEN_KEY, apiUrl } from '@/api/http'

/**
 * Subscreve SSE de progresso do documento via fetch (suporta Bearer JWT).
 * EventSource nativo não envia Authorization.
 *
 * Em 401/403 (token inválido após reset da BD, logout, etc.): limpa o token
 * e redirecciona para /login — evita AccessDenied silencioso e UI presa a 0%.
 *
 * @param {string} documentoId
 * @param {{ onProgress?: Function, onDone?: Function, onError?: Function }} handlers
 * @returns {() => void} cancelar
 */
export function subscreverProgressoDocumento(documentoId, handlers = {}) {
  const ctrl = new AbortController()
  const token = localStorage.getItem(TOKEN_KEY)

  ;(async () => {
    try {
      const res = await fetch(apiUrl(`/documentos/${documentoId}/progress`), {
        method: 'GET',
        headers: {
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        signal: ctrl.signal
      })

      if (res.status === 401 || res.status === 403) {
        try {
          localStorage.removeItem(TOKEN_KEY)
        } catch (_) {}
        handlers.onError?.({
          mensagem: 'Sessão expirada ou inválida. Faça login novamente.',
          codigo: 'AUTH_SSE',
          status: res.status
        })
        // Redirecionar só se ainda estivermos na app (não em testes)
        if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
          window.location.assign('/login')
        }
        return
      }

      if (!res.ok) {
        handlers.onError?.({ mensagem: `SSE HTTP ${res.status}` })
        return
      }

      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || ''

        for (const chunk of parts) {
          const lines = chunk.split('\n')
          let eventName = 'message'
          let data = ''
          for (const line of lines) {
            if (line.startsWith('event:')) eventName = line.slice(6).trim()
            else if (line.startsWith('data:')) data += line.slice(5).trim()
          }
          if (!data) continue
          let payload
          try {
            payload = JSON.parse(data)
          } catch {
            payload = { mensagem: data }
          }
          if (eventName === 'heartbeat') continue
          if (eventName === 'progress') handlers.onProgress?.(payload)
          else if (eventName === 'done') handlers.onDone?.(payload)
          else if (eventName === 'error') handlers.onError?.(payload)
        }
      }
    } catch (e) {
      if (e.name === 'AbortError') return
      handlers.onError?.({ mensagem: e.message || 'Ligação SSE interrompida' })
    }
  })()

  return () => ctrl.abort()
}
