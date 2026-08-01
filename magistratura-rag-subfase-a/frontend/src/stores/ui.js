import { defineStore } from 'pinia'
import { lerPrefsUi, gravarPrefsUi, normalizarPrefs } from '@/utils/prefsUi'

const CHAVE_COLAPSO = 'magistratura_sidebar_colapsada'

export const useUiStore = defineStore('ui', {
  state: () => ({
    sidebarColapsada: localStorage.getItem(CHAVE_COLAPSO) === '1',
    prefs: lerPrefsUi()
  }),
  getters: {
    mostrarDicas: (s) => s.prefs?.mostrarDicas !== false,
    guiaNivel: (s) => s.prefs?.guiaNivel || 'normal',
    confirmarAntesDeEliminar: (s) => s.prefs?.confirmarAntesDeEliminar !== false
  },
  actions: {
    definirSidebar(colapsada) {
      this.sidebarColapsada = !!colapsada
      localStorage.setItem(CHAVE_COLAPSO, this.sidebarColapsada ? '1' : '0')
    },
    alternarSidebar() {
      this.definirSidebar(!this.sidebarColapsada)
    },
    autoColapsarParaIA() {
      if (!this.sidebarColapsada) this.definirSidebar(true)
    },
    /**
     * Aplica preferências localmente e sincroniza o estado da sidebar
     * com "Menu lateral compacto".
     */
    aplicarPrefs(parcial = {}) {
      this.prefs = gravarPrefsUi(parcial)
      if (this.prefs.sidebarIniciaColapsada === true) {
        this.definirSidebar(true)
      } else if (parcial && Object.prototype.hasOwnProperty.call(parcial, 'sidebarIniciaColapsada')) {
        // Utilizador desligou explicitamente o modo compacto → expandir
        this.definirSidebar(false)
      }
      return this.prefs
    },
    hidratarPrefsDoServidor(serverPrefs = {}) {
      const merged = normalizarPrefs({ ...lerPrefsUi(), ...serverPrefs })
      this.prefs = gravarPrefsUi(merged)
      if (merged.sidebarIniciaColapsada === true) {
        this.definirSidebar(true)
      }
      return this.prefs
    },
    recarregarPrefsLocais() {
      this.prefs = lerPrefsUi()
      if (this.prefs.sidebarIniciaColapsada === true) {
        this.definirSidebar(true)
      }
    }
  }
})
