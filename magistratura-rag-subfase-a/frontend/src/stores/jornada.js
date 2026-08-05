import { defineStore } from 'pinia'
import { CAMINHADA_SEED_NORMALIZADO as CAMINHADA_SEED, ganchoParaMomento } from '@/jornada/seed'
import * as jornadaApi from '@/services/jornadaService'
import { emitGuideEvent, GuideEvent } from '@/guide/events'

const LS_PROGRESS = 'magistratura.jornada.progress'
const LS_HOOK = 'magistratura.jornada.hook'

function loadLocal() {
  try {
    const raw = localStorage.getItem(LS_PROGRESS)
    return raw ? JSON.parse(raw) : { momentoId: null, cenaId: null, concluidos: [] }
  } catch {
    return { momentoId: null, cenaId: null, concluidos: [] }
  }
}

function saveLocal(progress) {
  try {
    localStorage.setItem(LS_PROGRESS, JSON.stringify(progress))
  } catch {
    /* ignore */
  }
}

export const useJornadaStore = defineStore('jornada', {
  state: () => ({
    seed: CAMINHADA_SEED,
    progress: loadLocal(),
    bibliotecaPronta: true,
    diplomasCount: 1,
    carregando: false,
    erro: null
  }),

  getters: {
    momentosVisiveis(state) {
      return state.seed.momentos.filter((m) => {
        if (m.requerDoisDiplomas && state.diplomasCount < 2) return false
        if (m.requerDiplomaProcessado && !state.bibliotecaPronta) return false
        return true
      })
    },
    momentoActual(state) {
      const id = state.progress.momentoId || state.seed.momentos[0]?.id
      return state.seed.momentos.find((m) => m.id === id) || state.seed.momentos[0]
    },
    cenaActual() {
      const m = this.momentoActual
      if (!m) return null
      const cenaId = this.progress.cenaId
      if (cenaId) {
        const c = m.cenas.find((x) => x.id === cenaId)
        if (c) return c
      }
      return m.cenas[0]
    },
    cenaIndex() {
      const m = this.momentoActual
      const c = this.cenaActual
      if (!m || !c) return 0
      return Math.max(0, m.cenas.findIndex((x) => x.id === c.id))
    },
    isMomentoConcluido: (state) => (momentoId) =>
      (state.progress.concluidos || []).includes(momentoId),
    ganchoActual() {
      const m = this.momentoActual
      if (!m || this.isMomentoConcluido(m.id)) return null
      return ganchoParaMomento(m, this.cenaIndex)
    }
  },

  actions: {
    async carregar() {
      this.carregando = true
      this.erro = null
      try {
        const remoto = await jornadaApi.obterProgresso().catch(() => null)
        if (remoto) {
          this.progress = {
            momentoId: remoto.momentoId || this.progress.momentoId,
            cenaId: remoto.cenaId || this.progress.cenaId,
            concluidos: remoto.concluidos || this.progress.concluidos || []
          }
          saveLocal(this.progress)
        }
        const disp = await jornadaApi.obterDisponibilidade().catch(() => null)
        if (disp) {
          this.bibliotecaPronta = !!disp.temDiplomaProcessado
          this.diplomasCount = disp.diplomasProcessados ?? (this.bibliotecaPronta ? 1 : 0)
        }
        this.actualizarHook()
      } finally {
        this.carregando = false
      }
    },

    actualizarHook() {
      const hook = this.ganchoActual
      try {
        if (hook) {
          localStorage.setItem(LS_HOOK, hook)
        } else {
          localStorage.removeItem(LS_HOOK)
        }
      } catch {
        /* ignore */
      }
    },

    irParaMomento(momentoId) {
      const m = this.seed.momentos.find((x) => x.id === momentoId)
      if (!m) return
      this.progress.momentoId = m.id
      this.progress.cenaId = m.cenas[0]?.id || null
      saveLocal(this.progress)
      this.actualizarHook()
      this.persistir()
    },

    async avancarCena() {
      const m = this.momentoActual
      if (!m) return
      const idx = this.cenaIndex
      if (idx < m.cenas.length - 1) {
        this.progress.cenaId = m.cenas[idx + 1].id
        saveLocal(this.progress)
        this.actualizarHook()
        await this.persistir()
        return
      }
      const concluidos = new Set(this.progress.concluidos || [])
      concluidos.add(m.id)
      this.progress.concluidos = [...concluidos]
      // Próximo na sequência curricular (lista já ordenada por módulo + ordem)
      const lista = this.seed.momentos || []
      const idx = lista.findIndex((x) => x.id === m.id)
      const next = idx >= 0 ? lista[idx + 1] : null
      if (next) {
        this.progress.momentoId = next.id
        this.progress.cenaId = next.cenas?.[0]?.id || null
      }
      saveLocal(this.progress)
      this.actualizarHook()
      emitGuideEvent(GuideEvent.CHAPTER_COMPLETED, {
        mensagem: next
          ? 'Há mais uma história à espera — quando quiseres continuar.'
          : 'Por agora podes pausar. A Biblioteca e o Tutor continuam disponíveis.'
      })
      await this.persistir()
    },

    async persistir() {
      try {
        await jornadaApi.guardarProgresso({
          momentoId: this.progress.momentoId,
          cenaId: this.progress.cenaId,
          concluidos: this.progress.concluidos || []
        })
      } catch {
        /* localStorage basta */
      }
    }
  }
})
