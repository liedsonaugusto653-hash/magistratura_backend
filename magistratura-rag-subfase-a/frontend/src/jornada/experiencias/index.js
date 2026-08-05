/**
 * Agrega experiências (personagens) e expõe MOMENTOS ordenados.
 * O currículo (Missão → Módulo → Competência) vive em ../curriculo/.
 */
import * as joao from './joao.js'
import * as ana from './ana.js'

const MODULOS_NARRATIVOS = [joao, ana]

export const PERSONAGENS = Object.fromEntries(
  MODULOS_NARRATIVOS.map((m) => [m.PERSONAGEM.id, m.PERSONAGEM])
)

export const MOMENTOS = MODULOS_NARRATIVOS.flatMap((m) => m.MOMENTOS).sort((a, b) => {
  const ma = a.modulo ?? 0
  const mb = b.modulo ?? 0
  if (ma !== mb) return ma - mb
  return (a.ordem ?? 0) - (b.ordem ?? 0)
})

export const EXPERIENCIAS_JOAO = joao.MOMENTOS
export const EXPERIENCIAS_ANA = ana.MOMENTOS
