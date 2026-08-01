/**
 * Resolve CTAs da Caminhada para rotas reais da Biblioteca / Tutor.
 * Não inventa conteúdo jurídico — só navega para o que existir na BD.
 */
import bibliotecaService from '@/services/bibliotecaService'
import { heuristicaAncora } from '@/jornada/seed'

const PREFILL_KEY = 'tutor_prefill'

/**
 * Guarda prefill do Tutor (chave canónica usada por TutorView e ArtigoView).
 */
export function guardarPrefillTutor({ mensagem, diplomaId, artigoId, topicoId } = {}) {
  try {
    sessionStorage.setItem(
      PREFILL_KEY,
      JSON.stringify({
        mensagem: mensagem || '',
        diplomaId: diplomaId || null,
        artigoId: artigoId || null,
        topicoId: topicoId || null
      })
    )
  } catch {
    /* ignore */
  }
}

/**
 * Tenta encontrar um artigo real para a âncora lógica.
 * @returns {{ artigoId?: string, diplomaId?: string } | null}
 */
export async function resolverAncora(ancoraLogica) {
  if (!ancoraLogica) return null
  const h = heuristicaAncora(ancoraLogica)

  try {
    if (h.preferirOrdemMinima) {
      const diplomas = await bibliotecaService.listarDiplomas({ size: 30 })
      const lista = Array.isArray(diplomas) ? diplomas : diplomas?.content || []
      for (const d of lista) {
        const arts = await bibliotecaService.listarArtigos({ diplomaId: d.id, size: 5 })
        const content = arts?.content || arts || []
        if (content.length) {
          const ordenados = [...content].sort((a, b) => (a.ordem ?? 999) - (b.ordem ?? 999))
          const primeiro = ordenados[0]
          return {
            artigoId: primeiro.id,
            diplomaId: d.id
          }
        }
      }
      return null
    }

    for (const termo of h.termos || []) {
      const data = await bibliotecaService.listarArtigos({ termo, size: 10 })
      const content = data?.content || data || []
      if (content.length) {
        const a = content[0]
        return {
          artigoId: a.id,
          diplomaId: a.diplomaId || null
        }
      }
    }
  } catch {
    return null
  }
  return null
}

/**
 * Interpreta um CTA da cena e devolve destino de router + efeitos laterais.
 * @returns {Promise<{ path: string, query?: object, params?: object }>}
 */
export async function resolverCta(cta) {
  if (!cta) return { path: '/caminhada' }

  if (cta.tipo === 'abrir_tutor') {
    let diplomaId = cta.diplomaId || null
    let artigoId = cta.artigoId || null
    if (cta.ancoraLogica) {
      const r = await resolverAncora(cta.ancoraLogica)
      if (r) {
        diplomaId = r.diplomaId || diplomaId
        artigoId = r.artigoId || artigoId
      }
    }
    guardarPrefillTutor({
      mensagem: cta.prefill || '',
      diplomaId,
      artigoId
    })
    return { path: '/tutor' }
  }

  if (cta.tipo === 'abrir_artigo' || cta.ancoraLogica) {
    const r = cta.ancoraLogica ? await resolverAncora(cta.ancoraLogica) : null
    if (r?.artigoId) {
      return { path: `/biblioteca/artigos/${r.artigoId}` }
    }
    if (r?.diplomaId) {
      return { path: '/biblioteca', query: { diplomaId: r.diplomaId } }
    }
  }

  if (cta.to) {
    return { path: cta.to }
  }

  return { path: '/biblioteca' }
}
