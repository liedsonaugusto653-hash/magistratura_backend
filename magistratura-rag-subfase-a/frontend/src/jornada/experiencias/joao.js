/**
 * Missão 1 — Jornada do João (currículo narrativo orientado por competências)
 * Progressão cognitiva: ingenue → identifica → hipotese → argumenta → resolve
 * Fases: observar → compreender → interpretar → decidir
 */

export const PERSONAGEM = {
  id: 'joao',
  nome: 'João',
  ocupacao: 'estudante de Direito',
  resumo:
    'Acaba de entrar na Faculdade de Direito. Sonha ser magistrado desde pequeno. Ao longo desta missão, as suas perguntas passam de ingénuas a hipóteses e, depois, a argumentação sobre casos.'
}

/** @type {Array<Object>} */
export const MOMENTOS = [
{
  id: "joao-e1-porque-direito",
  personagemId: "joao",
  slug: "porque-existe-o-direito",
  titulo: "Porque existe o Direito?",
  modulo: 1,
  aula: 1,
  ordem: 1,
  fase: "iniciante",
  fasePedagogica: "observar",
  estadoCognitivo: "ingenue",
  competenciaId: "c1-conceito-direito",
  perguntaCentral: "Porque existe o Direito?",
  saidaEsperada: "O aluno consegue explicar, em linguagem simples, que o Direito organiza a vida em comum e limita o arbítrio.",
  ganchoProxima: "Mas quem cria estas regras? Qualquer pessoa pode inventar uma lei?",
  preRequisitos: [],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Função do Direito"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "João tinha acabado de entrar para a Faculdade de Direito. Desde pequeno, quando alguém lhe perguntava o que queria ser, a resposta era sempre a mesma: magistrado. Não era só uma profissão que admirava — era a ideia de decidir com justiça, com calma, com responsabilidade."
    },
    {
      tipo: "narrativa",
      texto: "Nas primeiras semanas, porém, sentiu um desconforto estranho. Os colegas falavam de códigos, de artigos, de “fontes”. Ele ainda estava numa pergunta mais antiga, quase embaraçosa de dizer em voz alta."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Se toda a gente fosse honesta… ainda seriam precisas leis?"
    },
    {
      tipo: "narrativa",
      texto: "Levou a dúvida para casa. À noite, viu no telejornal uma discussão sobre uma fila num serviço público, um desentendimento no trânsito, uma promessa não cumprida num contrato simples. Nada daquilo era “grande crime”. Mas em todos os casos alguém pedia uma regra clara — e alguém reclamava quando a regra falhava."
    },
    {
      tipo: "narrativa",
      texto: "Começou a perceber uma coisa simples: o Direito não existe só para castigar. Existe também para coordenar expectativas. Para que as pessoas saibam, com um mínimo de segurança, o que podem exigir umas das outras e o que o Estado pode exigir de cada um."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Então o Direito serve para viver juntos sem depender só da boa vontade de cada um."
    },
    {
      tipo: "narrativa",
      texto: "A frase não era ainda uma definição académica. Era um primeiro passo. João ainda não sabia quem escrevia as leis, nem o que tornava uma regra “jurídica”. Mas já não olhava para o Código como um livro mágico: olhava como uma tentativa humana de organizar a vida em comum."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Mas quem cria estas regras? Qualquer pessoa pode inventar uma lei?"
    }
  ],
  cta: {
    tipo: "abrir_biblioteca",
    label: "Explorar a Biblioteca",
    to: "/biblioteca"
  }
},{
  id: "joao-e2-quem-cria-regras",
  personagemId: "joao",
  slug: "quem-cria-as-regras",
  titulo: "Quem cria as regras?",
  modulo: 1,
  aula: 2,
  ordem: 2,
  fase: "iniciante",
  fasePedagogica: "observar",
  estadoCognitivo: "ingenue",
  competenciaId: "c2-direito-vs-moral",
  perguntaCentral: "Quem cria as regras — e o que as distingue da moral?",
  saidaEsperada: "O aluno distingue regras jurídicas de regras morais e de costumes, e reconhece que o Direito é produzido por autoridades competentes.",
  ganchoProxima: "Se o Direito tem regras próprias… o que é, afinal, uma norma jurídica?",
  preRequisitos: ["joao-e1-porque-direito"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Direito, moral e costume"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "No dia seguinte, João tentou testar a sua pergunta com um colega mais velho. Contou a ideia de que o Direito organiza a vida em comum. O colega sorriu e devolveu outra pergunta."
    },
    {
      tipo: "dialogo",
      quem: "Colega",
      texto: "A tua avó também tem regras em casa. Isso é Direito?"
    },
    {
      tipo: "narrativa",
      texto: "João ficou sem resposta. Em casa, havia regras: não mentir, cumprimentar as visitas, ajudar na mesa. Na igreja, havia outras. No bairro, costumes. No telemóvel, “regras” de um grupo de amigos. Tudo parecia norma — e nada parecia lei."
    },
    {
      tipo: "narrativa",
      texto: "Na aula, o professor falou sem rodeios: nem toda a regra é jurídica. A moral orienta a consciência; o costume orienta a convivência social; o Direito, quando é Direito em sentido estrito, é imposto por uma autoridade competente e pode ser exigido com coacção legítima do Estado."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Então não basta eu achar que algo é justo. É preciso saber quem pode transformar isso em regra obrigatória."
    },
    {
      tipo: "narrativa",
      texto: "A distinção ainda era grosseira, mas abria um caminho. João deixou de misturar, no mesmo saco, “o que a minha consciência manda”, “o que as pessoas costumam fazer” e “o que a lei exige”. Três mundos próximos — e diferentes."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Se o Direito tem regras próprias… o que é, afinal, uma norma jurídica?"
    }
  ],
  cta: {
    tipo: "abrir_biblioteca",
    label: "Ver diplomas na Biblioteca",
    to: "/biblioteca"
  }
},{
  id: "joao-e3-o-que-e-norma",
  personagemId: "joao",
  slug: "o-que-e-uma-norma",
  titulo: "O que é uma norma?",
  modulo: 1,
  aula: 3,
  ordem: 3,
  fase: "iniciante",
  fasePedagogica: "compreender",
  estadoCognitivo: "identifica",
  competenciaId: "c3-norma-juridica",
  perguntaCentral: "O que é uma norma jurídica?",
  saidaEsperada: "O aluno identifica a estrutura básica hipótese → consequência e reconhece uma norma como prescrição jurídica.",
  ganchoProxima: "Então… existem normas mais importantes do que outras?",
  preRequisitos: ["joao-e2-quem-cria-regras"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Estrutura da norma"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "João abriu a Constituição pela primeira vez com outro olhar. Já não procurava “a resposta para tudo”. Procurava padrões. Frases que mandavam, proibiam, autorizavam."
    },
    {
      tipo: "narrativa",
      texto: "O professor desenhou no quadro um esquema simples: se acontece um certo facto (hipótese), então segue-se uma certa consequência jurídica. Nem todas as normas se escrevem assim de forma explícita — mas o raciocínio do jurista muitas vezes começa aí."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Hipótese… e consequência. Como uma promessa que o ordenamento faz à sociedade."
    },
    {
      tipo: "narrativa",
      texto: "Exercitou com exemplos do quotidiano: se alguém celebra um contrato válido e não cumpre, pode haver responsabilidade; se alguém pratica certo facto típico ilícito e culposo com dano, pode haver dever de indemnizar. Ainda sem detalhe dogmático — só a forma do pensamento."
    },
    {
      tipo: "narrativa",
      texto: "Pela primeira vez, João sentiu que “ler a lei” não era decorar frases. Era reconhecer prescrições e perceber a que factos elas se ligam."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Então… existem normas mais importantes do que outras?"
    }
  ],
  cta: {
    tipo: "abrir_biblioteca",
    label: "Abrir um diploma na Biblioteca",
    to: "/biblioteca",
    ancoraLogica: "primeiro-artigo"
  }
},{
  id: "joao-e4-normas-superiores",
  personagemId: "joao",
  slug: "normas-superiores",
  titulo: "Porque existem normas superiores?",
  modulo: 1,
  aula: 4,
  ordem: 4,
  fase: "iniciante",
  fasePedagogica: "compreender",
  estadoCognitivo: "identifica",
  competenciaId: "c4-hierarquia-normas",
  perguntaCentral: "Porque existem normas superiores?",
  saidaEsperada: "O aluno explica a ideia de hierarquia normativa e porque uma norma inferior não pode contrariar uma superior.",
  ganchoProxima: "Se há uma norma no topo… essa norma é a Constituição?",
  preRequisitos: ["joao-e3-o-que-e-norma"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Hierarquia normativa"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "A pergunta de João não nasceu de um livro. Nasceu de uma conversa no minibus: alguém dizia que “a lei nova manda na lei velha”, outro respondia que “a Constituição manda em todas”. Os dois falavam com segurança. Os dois não podiam ter completamente razão da mesma forma."
    },
    {
      tipo: "narrativa",
      texto: "Na faculdade, o tema ganhou nome: hierarquia das normas. O ordenamento não é um saco de regras soltas. É um sistema em que algumas normas condicionam a validade e o sentido de outras."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Se uma regra inferior pudesse destruir a superior, o sistema deixava de ser sistema."
    },
    {
      tipo: "narrativa",
      texto: "Ainda não dominava os critérios de resolução de conflitos (hierarquia, especialidade, temporalidade). Mas já compreendia o ponto de partida: sem ordem entre normas, qualquer disputa se resolvia por força ou por acaso — e isso não é Direito."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Se há uma norma no topo… essa norma é a Constituição?"
    }
  ],
  cta: {
    tipo: "abrir_biblioteca",
    label: "Procurar a Constituição",
    to: "/biblioteca",
    ancoraLogica: "primeiro-artigo"
  }
},{
  id: "joao-e5-constituicao",
  personagemId: "joao",
  slug: "como-funciona-a-constituicao",
  titulo: "Como funciona a Constituição?",
  modulo: 1,
  aula: 5,
  ordem: 5,
  fase: "iniciante",
  fasePedagogica: "compreender",
  estadoCognitivo: "identifica",
  competenciaId: "c4-hierarquia-normas",
  perguntaCentral: "Como funciona a Constituição como norma suprema?",
  saidaEsperada: "O aluno reconhece a Constituição da República de Angola como norma fundamental e compreende, em traços gerais, o seu papel organizador do Estado e dos direitos.",
  ganchoProxima: "Se a Constituição organiza o Estado… quem exerce os poderes?",
  preRequisitos: ["joao-e4-normas-superiores"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Constituição"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "João voltou à livraria — desta vez sem a ansiedade de “aprender tudo”. Comprou de novo a Constituição da República de Angola, não como troféu, mas como mapa."
    },
    {
      tipo: "narrativa",
      texto: "Leu com calma os primeiros artigos. Percebeu que o texto não era só uma lista de bons desejos: organizava o poder, limitava o poder, e reconhecia direitos. Uma norma no topo não é apenas “a mais importante” — é a que define as regras do jogo para todas as outras."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "A Constituição não resolve todos os casos. Mas diz quem pode decidir e com que limites."
    },
    {
      tipo: "narrativa",
      texto: "Guardou no caderno uma frase curta, sem pretensão de doutrina fechada: a Constituição é o critério maior de validade e de orientação do resto do ordenamento. Tudo o que viesse a estudar depois — leis, decretos, regulamentos — teria de conversar com este texto."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Se a Constituição organiza o Estado… quem exerce os poderes?"
    }
  ],
  cta: {
    tipo: "abrir_biblioteca",
    label: "Abrir a Constituição na Biblioteca",
    to: "/biblioteca",
    ancoraLogica: "primeiro-artigo"
  }
},{
  id: "joao-e6-poderes-estado",
  personagemId: "joao",
  slug: "poderes-do-estado",
  titulo: "Quem exerce os poderes do Estado?",
  modulo: 1,
  aula: 6,
  ordem: 6,
  fase: "estudante",
  fasePedagogica: "compreender",
  estadoCognitivo: "hipotese",
  competenciaId: "c5-orgaos-estado",
  perguntaCentral: "Quem exerce os poderes do Estado?",
  saidaEsperada: "O aluno associa, em traços gerais, funções legislativa, executiva e judicial a órgãos e compreende a ideia de separação de poderes.",
  ganchoProxima: "Percebo quem decide… mas como se interpreta o que está escrito na norma?",
  preRequisitos: ["joao-e5-constituicao"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Separação de poderes"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "João passou um sábado a tentar desenhar o Estado numa folha. Assembleia. Presidente. Governo. Tribunais. Nomes que ouvia no noticiário ganharam, pela primeira vez, lugar num esquema."
    },
    {
      tipo: "narrativa",
      texto: "A ideia central não era decorar organogramas. Era compreender porque o poder não se concentra numa só mão: quem faz a lei não deve ser o mesmo que a aplica em todos os casos, e quem julga precisa de independência para não ser apenas eco do governo do dia."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Separar poderes não é desconfiança vazia. É técnica de liberdade."
    },
    {
      tipo: "narrativa",
      texto: "Ainda formulava hipóteses simples — “se o juiz dependesse do favor político, a norma virava ameaça”. Não era ainda um ensaio de Direito Constitucional. Era o início de um instinto de jurista: olhar para o órgão e perguntar qual a função e qual o limite."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Percebo quem decide… mas como se interpreta o que está escrito na norma?"
    }
  ],
  cta: {
    tipo: "abrir_tutor",
    label: "Perguntar ao Tutor sobre separação de poderes",
    mensagem: "Explica de forma clara o que significa separação de poderes na ordem constitucional angolana e porque importa para a independência dos tribunais."
  }
},{
  id: "joao-e7-interpretar",
  personagemId: "joao",
  slug: "como-interpretar-uma-norma",
  titulo: "Como interpretar uma norma?",
  modulo: 1,
  aula: 7,
  ordem: 7,
  fase: "estudante",
  fasePedagogica: "interpretar",
  estadoCognitivo: "argumenta",
  competenciaId: "c6-interpretar-norma",
  perguntaCentral: "Como interpretar uma norma?",
  saidaEsperada: "O aluno formula perguntas de interpretação (texto, contexto, finalidade) antes de concluir o sentido de uma disposição.",
  ganchoProxima: "Se já sei interrogar a norma… como a aplico a um facto concreto?",
  preRequisitos: ["joao-e6-poderes-estado"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Interpretação jurídica"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "Um professor leu em voz alta um artigo curto e perguntou à turma o que o texto “queria dizer”. Metade respondeu com segurança. A outra metade respondeu com segurança… no sentido oposto."
    },
    {
      tipo: "narrativa",
      texto: "João sentiu o chão mexer. Se a mesma frase gerava duas conclusões, decorar o artigo não bastava. Era preciso um método: ler o texto com atenção, situá-lo no diploma, perguntar qual o problema que a norma enfrenta, e só depois arriscar uma leitura."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Antes de defender uma interpretação, preciso de mostrar o caminho que usei para lá chegar."
    },
    {
      tipo: "narrativa",
      texto: "Começou a treinar perguntas em voz baixa: o que diz literalmente? Há definições no próprio diploma? Esta leitura esvazia a norma ou respeita a sua função? Ainda era um exercício principiante — mas já era argumentação, não palpites."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Se já sei interrogar a norma… como a aplico a um facto concreto?"
    }
  ],
  cta: {
    tipo: "abrir_tutor",
    label: "Pedir ao Tutor um exemplo de interpretação",
    mensagem: "Dá um exemplo simples de interpretação de uma norma jurídica: mostra o texto, as perguntas que um jurista faria e duas leituras possíveis com os seus riscos."
  }
},{
  id: "joao-e8-aplicar",
  personagemId: "joao",
  slug: "como-aplicar-uma-norma",
  titulo: "Como aplicar uma norma?",
  modulo: 1,
  aula: 8,
  ordem: 8,
  fase: "estudante",
  fasePedagogica: "decidir",
  estadoCognitivo: "argumenta",
  competenciaId: "c7-aplicar-norma",
  perguntaCentral: "Como aplicar uma norma a um facto?",
  saidaEsperada: "O aluno descreve o percurso facto → qualificação → norma → consequência, com uma justificativa mínima.",
  ganchoProxima: "Quero tentar um caso completo — mesmo que simples.",
  preRequisitos: ["joao-e7-interpretar"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Subsunção"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "Na prática de uma cadeira, o enunciado tinha três linhas: um facto, uma dúvida, um pedido de solução. João reconheceu o padrão que o professor tanto repetia."
    },
    {
      tipo: "narrativa",
      texto: "Primeiro, fixar o facto com honestidade — sem inventar o que não está no problema. Depois, perguntar qual a qualificação jurídica possível. Em seguida, localizar a norma candidata. Por fim, extrair a consequência e dizer porquê."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Aplicar não é “achar justo”. É mostrar o fio que liga o facto à norma e a norma à consequência."
    },
    {
      tipo: "narrativa",
      texto: "Errou detalhes. Corrigiu. Voltou a errar no rigor da linguagem. Mas pela primeira vez sentiu que estava a treinar o gestual de um futuro magistrado: decidir com fundamento, não com impulso."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "Quero tentar um caso completo — mesmo que simples."
    }
  ],
  cta: {
    tipo: "abrir_tutor",
    label: "Treinar facto → norma com o Tutor",
    mensagem: "Propõe-me um caso simples (2 ou 3 frases) e guia-me no raciocínio facto → norma → consequência. No fim, corrige a minha fundamentação."
  }
},{
  id: "joao-e9-caso-pratico",
  personagemId: "joao",
  slug: "pequeno-caso-pratico",
  titulo: "Pequeno caso prático",
  modulo: 1,
  aula: 9,
  ordem: 9,
  fase: "candidato",
  fasePedagogica: "decidir",
  estadoCognitivo: "resolve",
  competenciaId: "c7-aplicar-norma",
  perguntaCentral: "Consigo resolver um caso simples com fundamentação?",
  saidaEsperada: "O aluno percorre um caso elementar, formula uma solução provisória e identifica o que ainda precisa de estudar na Biblioteca.",
  ganchoProxima: "Fim da Missão 1 — Compreender o Direito. A próxima missão aprofundará fontes, interpretação e casos com mais variáveis.",
  preRequisitos: ["joao-e8-aplicar"],
  requerDiplomaProcessado: false,
  conceito: {
    rotulo: "Caso prático"
  },
  historia: [
    {
      tipo: "narrativa",
      texto: "O enunciado era deliberadamente simples. Um cidadão celebra um acordo verbal sobre a entrega de um bem. A outra parte atrasa-se sem justificação. Há prejuízo. João não tinha ainda o domínio de um código inteiro — e o exercício não pedia isso."
    },
    {
      tipo: "narrativa",
      texto: "Pediam-lhe o método: separar factos provados dos assumidos; perguntar se há relação jurídica relevante; identificar deveres possíveis; apontar onde iria procurar a norma; e só depois esboçar uma consequência."
    },
    {
      tipo: "dialogo",
      quem: "João",
      texto: "A minha conclusão é provisória. O que não é provisório é o dever de fundamentar."
    },
    {
      tipo: "narrativa",
      texto: "Quando terminou, comparou a sua folha com a de um colega. Discordaram num ponto. Em vez de impor a voz, João fez o que começara a aprender: pediu o artigo, o passo do raciocínio, o ponto em que os caminhos se separavam."
    },
    {
      tipo: "narrativa",
      texto: "Saiu da sala sem a ilusão de “já saber Direito”. Saiu com outra coisa, mais útil no início da caminhada: a consciência de que um futuro magistrado cresce cada vez que transforma uma impressão numa pergunta e uma pergunta num fundamento."
    },
    {
      tipo: "dialogo",
      quem: "Caderno",
      texto: "Missão 1: compreender o Direito — função, norma, hierarquia, órgãos, interpretação, aplicação. A seguir, aprofundar."
    }
  ],
  cta: {
    tipo: "abrir_tutor",
    label: "Rever o caso com o Tutor",
    mensagem: "Ajuda-me a rever um caso simples de incumprimento de acordo: quais as perguntas jurídicas essenciais e como estruturar uma resposta fundamentada para iniciantes."
  }
}
]
