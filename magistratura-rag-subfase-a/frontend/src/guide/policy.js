/**
 * Política do Guia de Estudo — orientação, não chatbot.
 * Sugere caminhos: Experiências, Biblioteca, Tutor, questões, flashcards, documentos.
 * Interativo, mas com cooldowns para não ser invasivo.
 */

const COOLDOWN_MS = {
  DEFAULT: 60_000,
  FIRST_LOGIN: 0,
  RETURN_WITH_MEMORY: 45_000,
  ENTER_LIBRARY: 90_000,
  QUESTION_ANSWERED: 75_000,
  DOCUMENT_PROCESSING: 30_000,
  DOCUMENT_FAILED: 15_000,
  EMPTY_SEARCH: 40_000,
  ARTICLE_OPENED: 120_000,
  ARTICLE_HAS_SCENE: 60_000,
  CHAPTER_COMPLETED: 30_000,
  IDLE_LONG: 180_000,
  GENERIC_ERROR: 25_000,
  SUGGEST_PRACTICE: 120_000,
  CONTINUE_WALK: 60_000,
  FEATURE_UNAVAILABLE: 45_000,
  NAVIGATE_AWAY_WHILE_PROCESSING: 20_000,
  SCENE_CTA: 40_000,
  IA_RATE_LIMITED: 45_000,
  IA_UNAVAILABLE: 45_000,
  TUTOR_NO_CONTEXT: 90_000
}

const lastShown = new Map()

const TEMPLATES = {
  FIRST_LOGIN: () => ({
    text: 'Bem-vindo. Podes começar pela Biblioteca, perguntar ao Tutor ou seguir as Experiências.',
    actions: [
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'tutor', label: 'Tutor IA', to: '/tutor' },
      { id: 'caminhada', label: 'Experiências', to: '/caminhada' },
      { id: 'ok', label: 'Explorar sozinho', dismiss: true }
    ],
    priority: 12
  }),
  RETURN_WITH_MEMORY: (p) => ({
    text: p.hook || 'Bom regresso. Podes retomar o estudo onde paraste.',
    actions: [
      { id: 'continuar', label: 'Continuar', to: p.to || '/dashboard' },
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'agora_nao', label: 'Agora não', dismiss: true }
    ],
    priority: 10
  }),
  ENTER_LIBRARY: (p) => ({
    text:
      p.mensagem ||
      'Na Biblioteca encontras diplomas e artigos. Abre um artigo para estudar, ou importa um PDF em Documentos.',
    actions: [
      { id: 'docs', label: 'Importar PDF', to: '/documentos' },
      { id: 'tutor', label: 'Tutor IA', to: '/tutor' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 5
  }),
  QUESTION_ANSWERED: (p) => {
    const ok = p.correta === true
    const base = ok
      ? 'Boa! Resposta correcta. Queres continuar a praticar ou rever o tema na Biblioteca?'
      : 'Não foi desta. Vale a pena rever o artigo ou pedir uma explicação ao Tutor.'
    return {
      text: p.mensagem || base,
      actions: ok
        ? [
            { id: 'mais', label: 'Mais questões', to: '/questoes' },
            { id: 'flash', label: 'Flashcards', to: '/flashcards' },
            { id: 'ok', label: 'Continuar', dismiss: true }
          ]
        : [
            { id: 'tutor', label: 'Explicar no Tutor', to: '/tutor' },
            { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
            { id: 'ok', label: 'Percebi', dismiss: true }
          ],
      priority: 7
    }
  },
  DOCUMENT_PROCESSING: (p) => ({
    text:
      p.mensagem ||
      'O documento está a ser processado. Podes continuar a estudar — avisamos quando estiver pronto.',
    actions: [
      { id: 'docs', label: 'Ver documentos', to: '/documentos' },
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 5
  }),
  DOCUMENT_FAILED: (p) => ({
    text: p.mensagem || 'O processamento deste documento falhou. Podes tentar novamente em Documentos.',
    actions: [
      { id: 'docs', label: 'Abrir Documentos', to: '/documentos' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 9
  }),
  EMPTY_SEARCH: (p) => ({
    text:
      p.mensagem ||
      'Não há resultados com esses termos. Experimenta outras palavras ou pergunta ao Tutor.',
    actions: [
      { id: 'tutor', label: 'Perguntar ao Tutor', to: '/tutor' },
      { id: 'biblioteca', label: 'Voltar à Biblioteca', to: '/biblioteca' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 6
  }),
  ARTICLE_OPENED: (p) => ({
    text:
      p.mensagem ||
      'Estás a ler um artigo. Podes pedir explicação ao Tutor ou gerar questões sobre este tema.',
    actions: [
      { id: 'tutor', label: 'Perguntar ao Tutor', to: '/tutor' },
      { id: 'questoes', label: 'Questões', to: '/questoes' },
      { id: 'ok', label: 'Só a ler', dismiss: true }
    ],
    priority: 4
  }),
  ARTICLE_HAS_SCENE: (p) => ({
    text: p.viu
      ? 'Há uma experiência ligada a este ponto. Também podes pedir uma explicação ao Tutor.'
      : 'Este artigo tem uma experiência nas Experiências. Queres vê-la?',
    actions: [
      { id: 'caminhada', label: 'Ver nas Experiências', to: p.to || '/caminhada' },
      { id: 'tutor', label: 'Tutor', to: '/tutor' },
      { id: 'agora_nao', label: 'Agora não', dismiss: true }
    ],
    priority: 7
  }),
  CHAPTER_COMPLETED: (p) => ({
    text: p.mensagem || 'Capítulo concluído. Podes continuar as Experiências ou consolidar com questões.',
    actions: [
      { id: 'continuar', label: 'Continuar', to: p.to || '/caminhada' },
      { id: 'questoes', label: 'Questões', to: '/questoes' },
      { id: 'ok', label: 'OK', dismiss: true }
    ],
    priority: 8
  }),
  IDLE_LONG: () => ({
    text: 'Ainda por aqui? Podes retomar: Biblioteca, Tutor, questões ou as Experiências nas Experiências.',
    actions: [
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'tutor', label: 'Tutor', to: '/tutor' },
      { id: 'caminhada', label: 'Experiências', to: '/caminhada' },
      { id: 'agora_nao', label: 'Agora não', dismiss: true }
    ],
    priority: 3
  }),
  FEATURE_UNAVAILABLE: (p) => ({
    text: p.mensagem || 'Ainda não está disponível — falta conteúdo processado na Biblioteca.',
    actions: [
      { id: 'docs', label: 'Importar documentos', to: '/documentos' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 6
  }),
  GENERIC_ERROR: (p) => ({
    text: p.mensagem || 'Aconteceu um pequeno problema. Podemos tentar de outro caminho.',
    actions: [
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 9
  }),
  CONTINUE_WALK: (p) => ({
    text: p.hook || 'Podes continuar as Experiências ou estudar o texto na Biblioteca.',
    actions: [
      { id: 'continuar', label: 'Continuar experiência', to: p.to || '/caminhada' },
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'agora_nao', label: 'Agora não', dismiss: true }
    ],
    priority: 8
  }),
  SCENE_CTA: (p) => ({
    text: p.mensagem || 'Há uma experiência pronta para este tema.',
    actions: [
      { id: 'ver', label: 'Ver experiência', to: p.to || '/caminhada' },
      { id: 'agora_nao', label: 'Agora não', dismiss: true }
    ],
    priority: 7
  }),
  NAVIGATE_AWAY_WHILE_PROCESSING: (p) => ({
    text: p.mensagem || 'O documento continua a processar em segundo plano. Podes voltar a Documentos a qualquer momento.',
    actions: [
      { id: 'docs', label: 'Documentos', to: '/documentos' },
      { id: 'ok', label: 'OK', dismiss: true }
    ],
    priority: 6
  }),
  SUGGEST_PRACTICE: (p) => ({
    text: p.mensagem || 'Já leste bastante texto. Que tal consolidar com questões ou flashcards?',
    actions: [
      { id: 'questoes', label: 'Questões', to: '/questoes' },
      { id: 'flash', label: 'Flashcards', to: '/flashcards' },
      { id: 'agora_nao', label: 'Agora não', dismiss: true }
    ],
    priority: 4
  }),
  IA_RATE_LIMITED: (p) => ({
    text:
      p.mensagem ||
      (p.retryAfterSeconds
        ? `A IA atingiu o limite de pedidos. Aguarda cerca de ${p.retryAfterSeconds}s ou estuda na Biblioteca entretanto.`
        : 'A IA atingiu o limite de pedidos. Aguarda um pouco ou continua na Biblioteca e nas questões.'),
    actions: [
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'questoes', label: 'Questões', to: '/questoes' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 11
  }),
  IA_UNAVAILABLE: (p) => ({
    text:
      p.mensagem ||
      'O Tutor IA está temporariamente indisponível. Podes continuar a estudar na Biblioteca.',
    actions: [
      { id: 'biblioteca', label: 'Biblioteca', to: '/biblioteca' },
      { id: 'ok', label: 'Percebi', dismiss: true }
    ],
    priority: 10
  }),
  TUTOR_NO_CONTEXT: (p) => ({
    text:
      p.mensagem ||
      'Sem diploma ou artigo seleccionado, o Tutor não cita legislação concreta da biblioteca. Escolhe um contexto acima para respostas ancoradas.',
    actions: [
      { id: 'biblioteca', label: 'Escolher na Biblioteca', to: '/biblioteca' },
      { id: 'ok', label: 'Continuar assim', dismiss: true }
    ],
    priority: 6
  })
}

export function evaluateGuideEvent(event, prefs = {}) {
  const nivel = prefs.guiaNivel || 'normal'
  if (nivel === 'desligado') return null

  const { type, payload } = event
  if (nivel === 'minimo') {
    const essenciais = new Set([
      'FIRST_LOGIN',
      'RETURN_WITH_MEMORY',
      'DOCUMENT_FAILED',
      'GENERIC_ERROR',
      'NAVIGATE_AWAY_WHILE_PROCESSING',
      'QUESTION_ANSWERED',
      'IA_RATE_LIMITED',
      'IA_UNAVAILABLE'
    ])
    if (!essenciais.has(type)) return null
  }

  const cooldown = COOLDOWN_MS[type] ?? COOLDOWN_MS.DEFAULT
  const last = lastShown.get(type) || 0
  if (Date.now() - last < cooldown) return null

  const factory = TEMPLATES[type]
  if (!factory) return null

  const msg = factory(payload || {})
  lastShown.set(type, Date.now())
  return {
    type,
    ...msg,
    at: event.at
  }
}

export function resetGuideCooldowns() {
  lastShown.clear()
}
