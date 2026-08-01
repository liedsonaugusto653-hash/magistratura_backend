import http from '@/api/http'

export default {
  obter() {
    return http.get('/estatisticas').then((r) => r.data)
  }
}
