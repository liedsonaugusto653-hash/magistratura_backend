import axios from 'axios'

/** Chave JWT — único ponto de acesso ao token no browser. */
export const TOKEN_KEY = 'magistratura_token'

/**
 * Base da API.
 * Produção:
 * - VITE_API_URL
 * - ou VITE_API_BASE_URL (compatibilidade)
 *
 * Exemplo:
 * VITE_API_URL=https://magistratura-backend.onrender.com/api
 */
export function getApiBaseUrl() {
  const raw = (
    import.meta.env.VITE_API_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    ''
  ).trim().replace(/\/$/, '')

  if (!raw) return '/api'

  return raw.endsWith('/api') ? raw : `${raw}/api`
}

/** URL absoluta para fetch/SSE. */
export function apiUrl(path) {
  const base = getApiBaseUrl()
  const p = path.startsWith('/') ? path : `/${path}`

  if (p.startsWith('/api/')) {
    return `${base.replace(/\/api$/, '')}${p}`
  }

  return `${base}${p}`
}

export function getAuthToken() {
  try {
    return localStorage.getItem(TOKEN_KEY) || null
  } catch {
    return null
  }
}

export function setAuthToken(token) {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  } catch {
    /* ignore */
  }
}

export function clearAuthToken() {
  setAuthToken(null)
}

const http = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json'
  }
})

http.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      clearAuthToken()

      const path = window.location.pathname

      if (
        path !== '/login' &&
        path !== '/registo' &&
        path !== '/recuperar-password' &&
        path !== '/redefinir-password'
      ) {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

/** Timeout longo para geração IA. */
export const IA_TIMEOUT_MS = 320_000

export default http