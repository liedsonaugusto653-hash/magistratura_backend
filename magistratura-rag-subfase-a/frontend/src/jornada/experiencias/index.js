import * as joao from './joao.js'
import * as ana from './ana.js'

const MODULOS = [joao, ana]

export const PERSONAGENS = Object.fromEntries(
  MODULOS.map((m) => [m.PERSONAGEM.id, m.PERSONAGEM])
)

export const MOMENTOS = MODULOS.flatMap((m) => m.MOMENTOS).sort((a, b) => {
  const ma = a.modulo ?? 0
  const mb = b.modulo ?? 0
  if (ma !== mb) return ma - mb
  return (a.ordem ?? 0) - (b.ordem ?? 0)
})
