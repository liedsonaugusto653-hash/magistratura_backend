/**
 * Ícones Lucide por iconKey (router meta.nav.iconKey).
 * Imports nomeados → tree-shaking do lucide-vue-next.
 */
import {
  LayoutDashboard,
  Sparkles,
  Wand2,
  Library,
  UploadCloud,
  Layers,
  ListChecks,
  LineChart,
  User,
  Settings,
  Network,
  Footprints
} from 'lucide-vue-next'

export const NAV_ICONS = {
  dashboard: LayoutDashboard,
  tutor: Sparkles,
  ferramentas: Wand2,
  biblioteca: Library,
  documentos: UploadCloud,
  flashcards: Layers,
  questoes: ListChecks,
  estatisticas: LineChart,
  mapa: Network,
  perfil: User,
  definicoes: Settings,
  caminhada: Footprints
}

export function iconForRoute(iconKey) {
  return NAV_ICONS[iconKey] || LayoutDashboard
}
