import { defineStore } from 'pinia'
import { evaluateGuideEvent, resetGuideCooldowns } from '@/guide/policy'
import { onGuideEvent } from '@/guide/events'
import { lerPrefsUi } from '@/utils/prefsUi'

const LS_VOZ = 'magistratura.guide.voz'

function lerVozPreferida() {
  try {
    const v = localStorage.getItem(LS_VOZ)
    if (v === null || v === undefined) return true
    return v === '1' || v === 'true'
  } catch {
    return true
  }
}

export const useGuideStore = defineStore('guide', {
  state: () => ({
    modo: 'quieto',
    mensagem: null,
    vozActiva: lerVozPreferida(),
    _unsub: null
  }),

  actions: {
    iniciar() {
      if (this._unsub) return
      this._unsub = onGuideEvent((event) => {
        const prefs = lerPrefsUi()
        const msg = evaluateGuideEvent(event, { guiaNivel: prefs.guiaNivel || 'normal' })
        if (!msg) return
        this.mostrar(msg)
      })
    },

    parar() {
      if (this._unsub) {
        this._unsub()
        this._unsub = null
      }
      resetGuideCooldowns()
    },

    mostrar(msg) {
      this.mensagem = msg
      this.modo = msg.priority >= 9 ? 'alerta' : 'fala'
    },

    dispensar() {
      this.mensagem = null
      this.modo = 'quieto'
    },

    definirVoz(activa) {
      this.vozActiva = !!activa
      try {
        localStorage.setItem(LS_VOZ, this.vozActiva ? '1' : '0')
      } catch {
        /* ignore */
      }
    },

    alternarVoz() {
      this.definirVoz(!this.vozActiva)
    }
  }
})
