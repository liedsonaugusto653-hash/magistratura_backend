export const PREFS_UI_KEY = 'magistratura.prefs.ui'

/** Schema canónico das preferências (local + servidor). */
export const PREFS_DEFAULTS = {
  sidebarIniciaColapsada: false,
  confirmarAntesDeEliminar: true,
  mostrarDicas: true,
  guiaNivel: 'normal', // normal | minimo | desligado
  tema: 'claro' // claro | escuro | sistema
}

export const GUIA_NIVEIS = [
  { value: 'normal', label: 'Normal', desc: 'Sugestões úteis, sem excesso.' },
  { value: 'minimo', label: 'Só essencial', desc: 'Apenas boas-vindas, erros e respostas a questões.' },
  { value: 'desligado', label: 'Desligado', desc: 'O guia não mostra mensagens.' }
]

export function normalizarPrefs(raw = {}) {
  const src = raw && typeof raw === 'object' ? raw : {}
  const guia = src.guiaNivel
  return {
    sidebarIniciaColapsada: !!src.sidebarIniciaColapsada,
    confirmarAntesDeEliminar: src.confirmarAntesDeEliminar !== false,
    mostrarDicas: src.mostrarDicas !== false,
    guiaNivel: ['normal', 'minimo', 'desligado'].includes(guia) ? guia : 'normal',
    tema: ['claro', 'escuro', 'sistema'].includes(src.tema) ? src.tema : 'claro'
  }
}

export function lerPrefsUi() {
  try {
    const raw = localStorage.getItem(PREFS_UI_KEY)
    if (!raw) return { ...PREFS_DEFAULTS }
    return normalizarPrefs({ ...PREFS_DEFAULTS, ...JSON.parse(raw) })
  } catch {
    return { ...PREFS_DEFAULTS }
  }
}

export function gravarPrefsUi(parcial = {}) {
  const next = normalizarPrefs({ ...lerPrefsUi(), ...parcial })
  localStorage.setItem(PREFS_UI_KEY, JSON.stringify(next))
  try {
    window.dispatchEvent(new CustomEvent('magistratura:prefs-ui', { detail: next }))
  } catch (_) {
    /* ignore */
  }
  return next
}

export function confirmarEliminacao(mensagem) {
  if (!lerPrefsUi().confirmarAntesDeEliminar) return true
  return window.confirm(mensagem)
}
