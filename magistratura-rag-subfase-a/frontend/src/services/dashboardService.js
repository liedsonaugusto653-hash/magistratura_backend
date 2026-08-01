import http from '@/api/http'

export default {
  obter() {
    return http.get('/dashboard').then((r) => r.data)
  }
}
