/**
 * Leitura em voz alta da narrativa do João (Web Speech API).
 */

const LANG = 'pt-PT'

export function partirEmFrases(texto) {
  if (!texto || !String(texto).trim()) return []
  const t = String(texto).trim()
  const partes = t.split(/(?<=[.!?…])\s+/)
  return partes.map((p) => p.trim()).filter(Boolean)
}

/**
 * Sequência linear de segmentos a partir dos blocos da cena.
 */
export function segmentosDaCena(blocos = []) {
  const segs = []
  ;(blocos || []).forEach((b, bi) => {
    const frases = partirEmFrases(b.texto)
    if (!frases.length && b.texto) {
      segs.push({ key: `${bi}-0`, tipo: b.tipo, quem: b.quem, texto: b.texto })
      return
    }
    frases.forEach((frase, fi) => {
      segs.push({ key: `${bi}-${fi}`, tipo: b.tipo, quem: b.quem, texto: frase })
    })
  })
  return segs
}

export function escolherVoz() {
  if (typeof window === 'undefined' || !window.speechSynthesis) return null
  const vozes = window.speechSynthesis.getVoices() || []
  const preferidas = [
    (v) => v.lang === 'pt-PT' || v.lang?.startsWith('pt-PT'),
    (v) => v.lang === 'pt-BR' || v.lang?.startsWith('pt-BR'),
    (v) => v.lang?.toLowerCase().startsWith('pt')
  ]
  for (const pred of preferidas) {
    const v = vozes.find(pred)
    if (v) return v
  }
  return vozes[0] || null
}

export function quandoVozesProntas() {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.speechSynthesis) {
      resolve([])
      return
    }
    const atuais = window.speechSynthesis.getVoices()
    if (atuais?.length) {
      resolve(atuais)
      return
    }
    const on = () => {
      window.speechSynthesis.removeEventListener('voiceschanged', on)
      resolve(window.speechSynthesis.getVoices() || [])
    }
    window.speechSynthesis.addEventListener('voiceschanged', on)
    setTimeout(() => {
      window.speechSynthesis.removeEventListener('voiceschanged', on)
      resolve(window.speechSynthesis.getVoices() || [])
    }, 800)
  })
}

/**
 * Motor de narração com suporte a avançar / recuar segmentos.
 * Não mistura reflexão — só lê os segmentos pedidos.
 */
export function criarNarrador(segmentos, { rate = 1, onIndex, onFim, onErro } = {}) {
  if (typeof window === 'undefined' || !window.speechSynthesis) {
    onErro?.(new Error('Leitura em voz alta não está disponível neste browser.'))
    return null
  }

  let cancelado = false
  let indice = 0
  let aCorrer = false
  const synth = window.speechSynthesis

  const textoDoSeg = (seg) =>
    seg.tipo === 'dialogo' && seg.quem ? `${seg.quem} disse: ${seg.texto}` : seg.texto

  const falarDesde = async (inicio) => {
    if (cancelado) return
    indice = Math.max(0, Math.min(inicio, segmentos.length))
    aCorrer = true
    synth.cancel()

    // Pequeno delay após cancel — Chrome precisa
    await new Promise((r) => setTimeout(r, 40))
    if (cancelado) return

    const passo = async () => {
      if (cancelado || !aCorrer) return
      if (indice >= segmentos.length) {
        aCorrer = false
        onIndex?.(-1)
        onFim?.()
        return
      }

      await quandoVozesProntas()
      if (cancelado || !aCorrer) return

      const seg = segmentos[indice]
      onIndex?.(indice)

      const utt = new SpeechSynthesisUtterance(textoDoSeg(seg))
      utt.lang = LANG
      utt.rate = rate
      utt.pitch = 1
      const voz = escolherVoz()
      if (voz) utt.voice = voz

      utt.onend = () => {
        if (cancelado || !aCorrer) return
        setTimeout(() => {
          if (cancelado || !aCorrer) return
          indice += 1
          passo()
        }, 280)
      }
      utt.onerror = (e) => {
        if (cancelado) return
        if (e?.error === 'interrupted' || e?.error === 'canceled') return
        aCorrer = false
        onErro?.(e)
      }

      synth.speak(utt)
    }

    passo()
  }

  return {
    iniciar(desde = 0) {
      cancelado = false
      return falarDesde(desde)
    },
    /** Salta para o segmento i e continua a ler a partir daí */
    irPara(i) {
      if (!segmentos.length) return
      const alvo = Math.max(0, Math.min(i, segmentos.length - 1))
      cancelado = false
      return falarDesde(alvo)
    },
    anterior() {
      const alvo = Math.max(0, indice - 1)
      cancelado = false
      return falarDesde(alvo)
    },
    seguinte() {
      const alvo = Math.min(segmentos.length - 1, indice + 1)
      // Se já no último e acabou, não faz nada
      cancelado = false
      return falarDesde(alvo)
    },
    pausar() {
      if (synth.speaking && !synth.paused) synth.pause()
    },
    retomar() {
      if (synth.paused) synth.resume()
    },
    parar() {
      cancelado = true
      aCorrer = false
      synth.cancel()
      onIndex?.(-1)
    },
    getIndice() {
      return indice
    },
    setRate(r) {
      rate = r
    }
  }
}

export function suporteTts() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/** Assinatura estável do conteúdo (evita cancelar TTS em re-renders). */
export function assinaturaBlocos(blocos = []) {
  return (blocos || []).map((b) => `${b.tipo}|${b.quem || ''}|${b.texto || ''}`).join('¦')
}
