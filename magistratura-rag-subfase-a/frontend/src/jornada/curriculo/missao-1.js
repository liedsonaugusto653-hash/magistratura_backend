/**
 * Missão 1 — Compreender o Direito
 * Currículo narrativo orientado por competências (Magistratura Angola).
 *
 * Hierarquia: Missão → Módulo → Competência → Experiência → Momento
 * O conteúdo narrativo vive em experiencias/joao.js (ids referenciados aqui).
 */

export const MISSAO = {
  id: 'missao-1',
  titulo: 'Compreender o Direito',
  descricao:
    'Construir a base conceptual e institucional do Direito angolano: porque existe, quem o cria, como se organiza e como se aplica a um caso simples.',
  ordem: 1,
  objectivoFormativo:
    'O estudante termina capaz de explicar o que é o Direito, localizar a Constituição na hierarquia normativa, reconhecer órgãos do Estado e aplicar uma norma a um facto simples com fundamentação mínima.'
}

export const MODULOS = [
  {
    id: 'mod-1-primeiros-passos',
    missaoId: 'missao-1',
    titulo: 'Primeiros Passos no Direito',
    ordem: 1,
    descricao:
      'Do primeiro contacto com a ideia de Direito até à aplicação de uma norma a um caso elementar.',
    fasePedagogicaInicio: 'observar',
    fasePedagogicaFim: 'decidir'
  }
]

export const COMPETENCIAS = [
  {
    id: 'c1-conceito-direito',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 1,
    titulo: 'Compreender o conceito de Direito',
    descricao: 'Explicar porque existe o Direito e distinguir a sua função social básica.',
    fasePedagogica: 'observar',
    experienciaIds: ['joao-e1-porque-direito']
  },
  {
    id: 'c2-direito-vs-moral',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 2,
    titulo: 'Distinguir Direito de Moral',
    descricao: 'Identificar diferenças entre regras jurídicas, morais e de cortesia.',
    fasePedagogica: 'observar',
    experienciaIds: ['joao-e2-quem-cria-regras']
  },
  {
    id: 'c3-norma-juridica',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 3,
    titulo: 'Identificar o que é uma norma jurídica',
    descricao: 'Reconhecer os elementos básicos de uma norma (hipótese e consequência).',
    fasePedagogica: 'compreender',
    experienciaIds: ['joao-e3-o-que-e-norma']
  },
  {
    id: 'c4-hierarquia-normas',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 4,
    titulo: 'Reconhecer a hierarquia das normas',
    descricao: 'Explicar porque existem normas superiores e o papel da Constituição.',
    fasePedagogica: 'compreender',
    experienciaIds: ['joao-e4-normas-superiores', 'joao-e5-constituicao']
  },
  {
    id: 'c5-orgaos-estado',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 5,
    titulo: 'Reconhecer os órgãos do Estado e a separação de poderes',
    descricao: 'Associar órgãos às funções legislativa, executiva e judicial.',
    fasePedagogica: 'compreender',
    experienciaIds: ['joao-e6-poderes-estado']
  },
  {
    id: 'c6-interpretar-norma',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 6,
    titulo: 'Interpretar uma norma de forma elementar',
    descricao: 'Formular perguntas de interpretação antes de concluir o sentido da norma.',
    fasePedagogica: 'interpretar',
    experienciaIds: ['joao-e7-interpretar']
  },
  {
    id: 'c7-aplicar-norma',
    moduloId: 'mod-1-primeiros-passos',
    ordem: 7,
    titulo: 'Aplicar uma norma a um caso simples',
    descricao: 'Percorrer o raciocínio facto → norma → consequência com fundamentação mínima.',
    fasePedagogica: 'decidir',
    experienciaIds: ['joao-e8-aplicar', 'joao-e9-caso-pratico']
  }
]

export const FASES_PEDAGOGICAS = {
  observar: {
    id: 'observar',
    ordem: 1,
    titulo: 'Observar',
    descricao: 'O aluno compreende. Ainda não decide.'
  },
  compreender: {
    id: 'compreender',
    ordem: 2,
    titulo: 'Compreender',
    descricao: 'Normas, fontes, órgãos e Constituição.'
  },
  interpretar: {
    id: 'interpretar',
    ordem: 3,
    titulo: 'Interpretar',
    descricao: 'Pequenos casos; justifica; usa Biblioteca e Tutor.'
  },
  decidir: {
    id: 'decidir',
    ordem: 4,
    titulo: 'Decidir',
    descricao: 'Casos completos; argumenta; recebe feedback fundamentado.'
  }
}

export const ESTADOS_COGNITIVOS = {
  ingenue: {
    id: 'ingenue',
    rotulo: 'Perguntas simples',
    descricao: 'João formula dúvidas básicas e confunde Direito com opinião ou moral.'
  },
  identifica: {
    id: 'identifica',
    rotulo: 'Identifica conceitos',
    descricao: 'Começa a nomear normas, fontes e órgãos com precisão crescente.'
  },
  hipotese: {
    id: 'hipotese',
    rotulo: 'Levanta hipóteses',
    descricao: 'Antecipa conflitos normativos e questiona hierarquia.'
  },
  argumenta: {
    id: 'argumenta',
    rotulo: 'Argumenta',
    descricao: 'Justifica interpretações com referência a texto e contexto.'
  },
  resolve: {
    id: 'resolve',
    rotulo: 'Resolve casos',
    descricao: 'Aplica facto → norma → consequência e aceita contra-argumentos.'
  }
}
