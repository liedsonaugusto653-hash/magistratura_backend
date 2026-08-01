import http from '@/api/http'

/**
 * API mínima da Caminhada.
 * Se o backend ainda não tiver os endpoints, o store faz fallback a localStorage.
 * baseURL de http já é `/api` — paths relativos sem prefixo duplicado.
 */
export async function obterProgresso() {
  const { data } = await http.get('/jornada/progresso')
  return data
}

export async function guardarProgresso(payload) {
  const { data } = await http.put('/jornada/progresso', payload)
  return data
}

export async function obterDisponibilidade() {
  const { data } = await http.get('/jornada/disponibilidade')
  return data
}
