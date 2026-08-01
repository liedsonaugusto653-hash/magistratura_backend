/**
 * Arte SVG do sistema por iconKey (router meta.nav.iconKey).
 * Alinhado com FeatureArt / PageHero — mesma linguagem em todo o produto.
 */
export const NAV_ART = {
  dashboard: 'default',
  caminhada: 'caminhada',
  tutor: 'tutor',
  mapa: 'mapa',
  ferramentas: 'tools',
  biblioteca: 'biblioteca',
  documentos: 'biblioteca',
  flashcards: 'flashcards',
  questoes: 'questoes',
  estatisticas: 'stats',
  perfil: 'perfil',
  definicoes: 'settings',
  simulados: 'simulados'
}

export function artForRoute(iconKey) {
  return NAV_ART[iconKey] || 'default'
}
