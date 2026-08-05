/**
 * Sistema narrativo — Magistratura
 * Conteúdo: src/jornada/experiencias/*
 * Currículo: src/jornada/curriculo/*
 * Progresso: localStorage + API /api/jornada
 */
import { PERSONAGENS as PERSONAGENS_MOD, MOMENTOS } from './experiencias/index.js'
import {
  MISSAO,
  MODULOS,
  COMPETENCIAS,
  FASES_PEDAGOGICAS,
  ESTADOS_COGNITIVOS,
  anexarMetadadosCurriculo,
  competenciaDeExperiencia
} from './curriculo/index.js'

export const PERSONAGENS = PERSONAGENS_MOD

export {
  MISSAO,
  MODULOS,
  COMPETENCIAS,
  FASES_PEDAGOGICAS,
  ESTADOS_COGNITIVOS,
  anexarMetadadosCurriculo,
  competenciaDeExperiencia
}

export const CAMINHADA_SEED = {
  id: 'curriculo-missao-1-v1',
  titulo: 'Currículo narrativo — Missão 1',
  missaoId: MISSAO.id,
  momentos: MOMENTOS
}

export function blocosDoMomento(momento, cenaIndex = 0) {
  if (!momento) return []
  if (Array.isArray(momento.historia) && momento.historia.length) {
    return momento.historia
  }
  const cena = momento.cenas?.[cenaIndex] || momento.cenas?.[0]
  return cena?.blocos || []
}

export function ctaDoMomento(momento) {
  if (!momento) return null
  if (momento.cta) return momento.cta
  const cena = momento.cenas?.[momento.cenas.length - 1]
  return cena?.cta || null
}

export function ganchoParaMomento(momento, cenaIndex) {
  if (!momento) return null
  if (momento.ganchoProxima && (cenaIndex == null || cenaIndex === 0)) {
    const quem = nomePersonagem(momento)
    return quem ? `${quem}: «${momento.ganchoProxima}»` : momento.ganchoProxima
  }
  const quem = nomePersonagem(momento)
  const trecho = quem ? `${quem} · «${momento.titulo}»` : `«${momento.titulo}»`
  if (cenaIndex == null || cenaIndex === 0) return `Podes continuar: ${trecho}.`
  return `Ficaste a meio de ${trecho}.`
}

export function nomePersonagem(momento) {
  if (!momento) return null
  if (momento.personagemNome) return momento.personagemNome
  const id = momento.personagemId
  if (id && PERSONAGENS[id]?.nome) return PERSONAGENS[id].nome
  const falas = (momento.historia || []).filter((b) => b.tipo === 'dialogo' && b.quem)
  if (falas.length) return falas[0].quem
  return null
}

export function reflexaoParaMomento(momento) {
  if (!momento) return null
  if (momento.perguntaCentral) {
    return {
      titulo: 'Pergunta central',
      texto: momento.perguntaCentral
    }
  }
  return null
}

export function heuristicaAncora(ancoraLogica) {
  switch (ancoraLogica) {
    case 'primeiro-artigo':
      return { preferirOrdemMinima: true, termos: [] }
    case 'direitos-fundamentais':
      return {
        preferirOrdemMinima: false,
        termos: ['direitos fundamentais', 'liberdade', 'dignidade', 'igualdade']
      }
    case 'trabalho':
      return {
        preferirOrdemMinima: false,
        termos: ['trabalho', 'trabalhador', 'contrato de trabalho', 'despedimento']
      }
    default:
      return { preferirOrdemMinima: false, termos: [] }
  }
}

export function momentoParaArtigo() {
  return null
}

export function normalizarSeed(seed = CAMINHADA_SEED) {
  const momentos = (seed.momentos || []).map((m) => {
    const base =
      m.cenas && m.cenas.length
        ? m
        : {
            ...m,
            cenas: [{ id: `${m.id}-c1`, ordem: 1, blocos: m.historia || [], cta: m.cta || null }]
          }
    return anexarMetadadosCurriculo(base)
  })
  return {
    ...seed,
    momentos,
    personagens: PERSONAGENS,
    missao: MISSAO,
    modulos: MODULOS,
    competencias: COMPETENCIAS
  }
}

export const CAMINHADA_SEED_NORMALIZADO = normalizarSeed(CAMINHADA_SEED)

export function progressoCompetencias(concluidosIds = []) {
  const set = new Set(concluidosIds || [])
  return COMPETENCIAS.map((c) => {
    const total = (c.experienciaIds || []).length
    const feitas = (c.experienciaIds || []).filter((id) => set.has(id)).length
    return {
      ...c,
      total,
      feitas,
      completa: total > 0 && feitas >= total,
      percentagem: total ? Math.round((feitas / total) * 100) : 0
    }
  })
}
