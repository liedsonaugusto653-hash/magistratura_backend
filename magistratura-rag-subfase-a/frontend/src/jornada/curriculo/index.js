/**
 * Currículo narrativo — ponto de entrada.
 * Missão → Módulo → Competência → Experiência (momento) → blocos
 */
import {
  MISSAO,
  MODULOS,
  COMPETENCIAS,
  FASES_PEDAGOGICAS,
  ESTADOS_COGNITIVOS
} from './missao-1.js'

export { MISSAO, MODULOS, COMPETENCIAS, FASES_PEDAGOGICAS, ESTADOS_COGNITIVOS }

export const MISSOES = [MISSAO]

export const COMPETENCIAS_POR_ID = Object.fromEntries(
  COMPETENCIAS.map((c) => [c.id, c])
)

export const COMPETENCIA_POR_EXPERIENCIA = (() => {
  const map = {}
  for (const c of COMPETENCIAS) {
    for (const eid of c.experienciaIds || []) {
      map[eid] = c
    }
  }
  return map
})()

export function competenciaDeExperiencia(experienciaId) {
  return COMPETENCIA_POR_EXPERIENCIA[experienciaId] || null
}

export function moduloDeCompetencia(competenciaId) {
  const c = COMPETENCIAS_POR_ID[competenciaId]
  if (!c) return null
  return MODULOS.find((m) => m.id === c.moduloId) || null
}

export function missaoDeModulo(moduloId) {
  const mod = MODULOS.find((m) => m.id === moduloId)
  if (!mod) return null
  return MISSOES.find((m) => m.id === mod.missaoId) || null
}

export const ORDEM_EXPERIENCIAS_MISSAO_1 = COMPETENCIAS.flatMap((c) => c.experienciaIds)

export function anexarMetadadosCurriculo(momento) {
  if (!momento) return momento
  const comp = momento.competenciaId
    ? COMPETENCIAS_POR_ID[momento.competenciaId]
    : competenciaDeExperiencia(momento.id)
  const modulo = comp ? moduloDeCompetencia(comp.id) : null
  const missao = modulo ? missaoDeModulo(modulo.id) : null
  const faseMeta = momento.fasePedagogica
    ? FASES_PEDAGOGICAS[momento.fasePedagogica]
    : comp
      ? FASES_PEDAGOGICAS[comp.fasePedagogica]
      : null
  const estadoMeta = momento.estadoCognitivo
    ? ESTADOS_COGNITIVOS[momento.estadoCognitivo]
    : null

  return {
    ...momento,
    competencia: comp || null,
    modulo: modulo || null,
    missao: missao || null,
    fasePedagogicaMeta: faseMeta || null,
    estadoCognitivoMeta: estadoMeta || null
  }
}

export const GUIA_EXTENSAO = {
  passos: [
    'Definir a competência (verbo de acção: compreender, distinguir, aplicar…)',
    'Escrever a experiência com perguntaCentral, saidaEsperada, ganchoProxima',
    'Ligar experienciaId à competência no currículo',
    'Validar pré-requisitos e ordem',
    'Testar CTAs (Biblioteca / Tutor) e progresso'
  ]
}
