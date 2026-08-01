/**
 * Bus de eventos do Guia de Estudo.
 * O Guia nunca fala sozinho — só reage a eventos emitidos pela app.
 * Textos são templates curtos; não há LLM no Guia.
 */

const listeners = new Set()

export const GuideEvent = Object.freeze({
  FIRST_LOGIN: 'FIRST_LOGIN',
  RETURN_WITH_MEMORY: 'RETURN_WITH_MEMORY',
  ENTER_LIBRARY: 'ENTER_LIBRARY',
  QUESTION_ANSWERED: 'QUESTION_ANSWERED',
  DOCUMENT_PROCESSING: 'DOCUMENT_PROCESSING',
  DOCUMENT_FAILED: 'DOCUMENT_FAILED',
  EMPTY_SEARCH: 'EMPTY_SEARCH',
  ARTICLE_OPENED: 'ARTICLE_OPENED',
  ARTICLE_HAS_SCENE: 'ARTICLE_HAS_SCENE',
  CHAPTER_COMPLETED: 'CHAPTER_COMPLETED',
  IDLE_LONG: 'IDLE_LONG',
  FEATURE_UNAVAILABLE: 'FEATURE_UNAVAILABLE',
  NAVIGATE_AWAY_WHILE_PROCESSING: 'NAVIGATE_AWAY_WHILE_PROCESSING',
  GENERIC_ERROR: 'GENERIC_ERROR',
  CONTINUE_WALK: 'CONTINUE_WALK',
  SCENE_CTA: 'SCENE_CTA',
  SUGGEST_PRACTICE: 'SUGGEST_PRACTICE',
  IA_RATE_LIMITED: 'IA_RATE_LIMITED',
  IA_UNAVAILABLE: 'IA_UNAVAILABLE',
  TUTOR_NO_CONTEXT: 'TUTOR_NO_CONTEXT'
})

export function emitGuideEvent(type, payload = {}) {
  const event = { type, payload, at: Date.now() }
  listeners.forEach((fn) => {
    try {
      fn(event)
    } catch (e) {
      console.warn('[guide] listener error', e)
    }
  })
  return event
}

export function onGuideEvent(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}
