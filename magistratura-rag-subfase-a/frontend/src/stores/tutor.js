import { defineStore } from 'pinia'
import tutorService from '@/services/tutorService'
import { emitGuideEvent, GuideEvent } from '@/guide/events'

function dedupeConversas(lista) {
  const seen = new Set()
  const out = []
  for (const c of lista || []) {
    const id = c?.id
    if (!id || seen.has(id)) continue
    seen.add(id)
    out.push(c)
  }
  return out
}

export const useTutorStore = defineStore('tutor', {
  state: () => ({
    conversas: [],
    conversaAtual: null,
    mensagens: [],
    aEnviar: false,
    aCarregarConversas: false,
    aCarregarConversa: false,
    iaDisponivel: null,
    providerNome: '',
    erro: null,
    contextoDiplomaId: null,
    contextoArtigoId: null,
    contextoTopicoId: null,
    localLimitPerMinute: null,
    localUsed: null,
    localRemaining: null,
    upstreamRateLimited: false,
    retryAfterSeconds: null
  }),

  actions: {
    definirContexto({ diplomaId = null, artigoId = null, topicoId = null } = {}) {
      this.contextoDiplomaId = diplomaId || null
      this.contextoArtigoId = artigoId || null
      this.contextoTopicoId = topicoId || null
    },

    limparContexto() {
      this.contextoDiplomaId = null
      this.contextoArtigoId = null
      this.contextoTopicoId = null
    },

    notificarErroIa(err) {
      const msg = (err?.response?.data?.error || err?.response?.data?.message || err?.message || String(err || '')).toString()
      const status = err?.response?.status
      const lower = msg.toLowerCase()
      const is429 = status === 429 || lower.includes('429') || lower.includes('limite') || lower.includes('rate')
      const is503 = status === 503 || lower.includes('503') || lower.includes('indispon')
      if (is429) {
        const retry = err?.response?.data?.retryAfterSeconds || this.retryAfterSeconds || 60
        this.upstreamRateLimited = true
        this.retryAfterSeconds = retry
        this.erro = msg.includes('Limite') ? msg : `Limite da IA atingido. Tenta daqui a ~${retry}s.`
        try { emitGuideEvent(GuideEvent.IA_RATE_LIMITED, { retryAfterSeconds: retry, mensagem: this.erro }) } catch {}
      } else if (is503 || lower.includes('http')) {
        this.erro = msg || 'Tutor IA temporariamente indisponível.'
        try { emitGuideEvent(GuideEvent.IA_UNAVAILABLE, { mensagem: this.erro }) } catch {}
      } else {
        this.erro = msg || 'Não foi possível obter resposta do Tutor.'
      }
      this.verificarStatus()
    },

    async verificarStatus() {
      try {
        const s = await tutorService.status()
        this.iaDisponivel = s.disponivel
        this.providerNome = s.provider
        this.localLimitPerMinute = s.localLimitPerMinute ?? null
        this.localUsed = s.localUsed ?? null
        this.localRemaining = s.localRemaining ?? null
        this.upstreamRateLimited = !!s.upstreamRateLimited
        this.retryAfterSeconds = s.retryAfterSeconds ?? null
        if (s.upstreamRateLimited) {
          try {
            emitGuideEvent(GuideEvent.IA_RATE_LIMITED, {
              retryAfterSeconds: s.retryAfterSeconds,
              mensagem: s.upstreamMessage
            })
          } catch { /* guide best-effort */ }
        }
      } catch {
        this.iaDisponivel = false
      }
    },

    async carregarConversas() {
      this.aCarregarConversas = true
      try {
        const lista = await tutorService.listarConversas()
        this.conversas = dedupeConversas(lista)
      } finally {
        this.aCarregarConversas = false
      }
    },

    async criarConversa(titulo) {
      const conversa = await tutorService.criarConversa(titulo)
      this.conversas = dedupeConversas([conversa, ...this.conversas])
      await this.selecionarConversa(conversa.id)
      return conversa
    },

    async selecionarConversa(id) {
      if (!id) return
      this.aCarregarConversa = true
      this.erro = null
      try {
        const detalhe = await tutorService.obterConversa(id)
        this.conversaAtual = detalhe
        this.mensagens = detalhe.mensagens || []
      } finally {
        this.aCarregarConversa = false
      }
    },

    async eliminarConversa(id) {
      await tutorService.eliminarConversa(id)
      this.conversas = this.conversas.filter((c) => c.id !== id)
      if (this.conversaAtual?.id === id) {
        this.conversaAtual = null
        this.mensagens = []
      }
    },

    _ligarConversaStream(id, tituloSugestao) {
      if (!id) return
      const titulo = (tituloSugestao || this.conversaAtual?.titulo || 'Nova conversa').slice(0, 60)
      // Sempre garantir id na conversa actual (mesmo se já existia objecto parcial)
      this.conversaAtual = {
        ...(this.conversaAtual || {}),
        id,
        titulo: this.conversaAtual?.titulo && this.conversaAtual.titulo !== 'Nova conversa'
          ? this.conversaAtual.titulo
          : titulo
      }
      if (!this.conversas.some((c) => c.id === id)) {
        this.conversas = dedupeConversas([
          {
            id,
            titulo: this.conversaAtual.titulo,
            dataCriacao: new Date().toISOString(),
            dataAtualizacao: new Date().toISOString()
          },
          ...this.conversas
        ])
      }
    },

    /**
     * Actualiza lista em fundo. NÃO bloqueia o botão Enviar.
     * NÃO limpa conversaAtual se o GET falhar.
     */
    _sincronizarEmFundo(conversaId) {
      const id = conversaId || this.conversaAtual?.id
      // fire-and-forget
      ;(async () => {
        try {
          await this.carregarConversas()
        } catch {
          /* ignore */
        }
        if (!id || this.aEnviar) return
        try {
          const detalhe = await tutorService.obterConversa(id)
          // Só aplica se o utilizador ainda estiver na mesma conversa e não a enviar
          if (this.aEnviar) return
          if (this.conversaAtual?.id && this.conversaAtual.id !== id) return
          this.conversaAtual = detalhe
          this.mensagens = detalhe.mensagens || []
        } catch {
          /* mantém mensagens optimistas / stream já visíveis */
        }
      })()
    },

    async enviarMensagem(mensagem) {
      if (this.aEnviar) return
      const texto = (mensagem || '').trim()
      if (!texto) return

      this.erro = null
      this.aEnviar = true

      if (!this.contextoDiplomaId && !this.contextoArtigoId) {
        try {
          emitGuideEvent(GuideEvent.TUTOR_NO_CONTEXT, {})
        } catch { /* best-effort */ }
      }

      const otimista = {
        id: `temp-${Date.now()}`,
        autor: 'UTILIZADOR',
        conteudo: texto,
        timestamp: new Date().toISOString()
      }
      this.mensagens.push(otimista)

      const contexto = {
        diplomaId: this.contextoDiplomaId || undefined,
        artigoId: this.contextoArtigoId || undefined,
        topicoId: this.contextoTopicoId || undefined
      }

      let conversaIdEfectivo = this.conversaAtual?.id || null
      let respostaOk = false

      try {
        try {
          let acumulado = ''
          const placeholder = {
            id: `stream-${Date.now()}`,
            autor: 'IA',
            conteudo: '',
            timestamp: new Date().toISOString(),
            aEscrever: true
          }
          this.mensagens.push(placeholder)
          const indicePlaceholder = this.mensagens.length - 1

          await tutorService.chatStream(
            { conversaId: conversaIdEfectivo, mensagem: texto, ...contexto },
            (chunk) => {
              if (chunk === 'true' || chunk === 'false') return
              acumulado += chunk
              if (this.mensagens[indicePlaceholder]) {
                this.mensagens[indicePlaceholder].conteudo = acumulado
              }
            },
            (fontes) => {
              if (this.mensagens[indicePlaceholder]) {
                this.mensagens[indicePlaceholder].fontes = fontes || []
              }
            },
            (novaConversaId) => {
              conversaIdEfectivo = novaConversaId
              this._ligarConversaStream(novaConversaId, texto)
            }
          )

          if (this.mensagens[indicePlaceholder]) {
            this.mensagens[indicePlaceholder].aEscrever = false
            this.mensagens[indicePlaceholder].conteudo = acumulado
          }
          respostaOk = true
        } catch (streamErr) {
          this.mensagens = this.mensagens.filter((m) => !m.aEscrever)

          if (!conversaIdEfectivo) {
            try {
              await this.carregarConversas()
              if (this.conversas[0]?.id) {
                conversaIdEfectivo = this.conversas[0].id
                this._ligarConversaStream(conversaIdEfectivo, texto)
              }
            } catch {
              /* fallback */
            }
          }
          console.warn('[tutor] stream falhou, a tentar síncrono', streamErr?.message)

          const resposta = await tutorService.chat({
            conversaId: conversaIdEfectivo,
            mensagem: texto,
            ...contexto
          })
          const corpo =
            typeof resposta === 'string'
              ? resposta
              : resposta?.conteudo || resposta?.texto || JSON.stringify(resposta)

          this.mensagens.push({
            id: `ia-${Date.now()}`,
            autor: 'IA',
            conteudo: corpo,
            timestamp: new Date().toISOString(),
            fontes: Array.isArray(resposta?.fontes) ? resposta.fontes : []
          })

          if (resposta?.conversaId) {
            conversaIdEfectivo = resposta.conversaId
            this._ligarConversaStream(conversaIdEfectivo, texto)
          }
          respostaOk = true
        }
      } catch (err) {
        this.notificarErroIa(err)
        this.mensagens = this.mensagens.filter((m) => m.id !== otimista.id && !m.aEscrever)
      } finally {
        // Sempre libertar o envio — mesmo se o sync em fundo falhar
        this.aEnviar = false
      }

      // Sync em fundo (não await) — o utilizador pode enviar a 2.ª mensagem de imediato
      if (respostaOk) {
        if (conversaIdEfectivo) {
          this._ligarConversaStream(conversaIdEfectivo, texto)
        }
        this._sincronizarEmFundo(conversaIdEfectivo)
      }
    }
  }
})
