/**
 * Sistema narrativo — Magistratura
 * Conteúdo: src/jornada/experiencias/*
 * Progresso: localStorage (magistratura.jornada.progress)
 */
import { PERSONAGENS as PERSONAGENS_MOD, MOMENTOS } from './experiencias/index.js'

export const PERSONAGENS = PERSONAGENS_MOD

export const CAMINHADA_SEED = {
  id: 'experiencias-v3-joao-ana',
  titulo: 'Experiências',
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

export function reflexaoParaMomento() {
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
    if (m.cenas && m.cenas.length) return m
    return {
      ...m,
      cenas: [{ id: `${m.id}-c1`, ordem: 1, blocos: m.historia || [], cta: m.cta || null }]
    }
  })
  return { ...seed, momentos, personagens: PERSONAGENS }
}

export const CAMINHADA_SEED_NORMALIZADO = normalizarSeed(CAMINHADA_SEED)
