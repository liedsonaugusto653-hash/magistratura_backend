/**
 * Módulo 1 — João
 * Aula 1: A primeira página
 * Aula 2: O erro de tentar aprender tudo ao mesmo tempo
 * Aula 3: Saber a resposta ou compreender a resposta
 * Aula 4: O dia em que João descobriu que uma lei não vive sozinha
 * Aula 5: O dia em que João percebeu que decorar não era aprender
 * Aula 6: O dia em que João descobriu que Direito não era apenas uma questão de opinião
 */

export const PERSONAGEM = {
  id: 'joao',
  nome: 'João',
  ocupacao: 'estudante de Direito',
  resumo: 'Acaba de entrar na Faculdade de Direito. Sonha ser magistrado desde pequeno.'
}

/** @type {Array<Object>} */
export const MOMENTOS = [
  {
    id: 'joao-m1-a1',
    personagemId: 'joao',
    slug: 'a-primeira-pagina',
    titulo: 'A primeira página',
    modulo: 1,
    aula: 1,
    ordem: 1,
    fase: 'iniciante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'João tinha acabado de entrar para a Faculdade de Direito e, desde pequeno, quando alguém lhe perguntava o que queria ser no futuro, a resposta era sempre a mesma: queria ser magistrado. Não era apenas uma profissão que admirava, era um sonho que foi crescendo dentro dele durante anos, alimentado pela vontade de compreender as leis, defender a justiça e um dia ter a responsabilidade de tomar decisões importantes na vida das pessoas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'As aulas tinham começado há poucos dias, mas João não gostava da ideia de esperar. Sentia que estava atrasado, como se cada dia que passasse sem estudar fosse um dia perdido na construção do seu sonho. Enquanto alguns colegas ainda tentavam adaptar-se à nova rotina da faculdade, ele já queria ir mais longe, queria conhecer as leis, entender como funcionava o mundo jurídico e começar a preparar-se para o futuro que sempre imaginou.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Foi por isso que, numa tarde de sábado, entrou numa livraria e comprou a Constituição da República. Enquanto caminhava pelos corredores, viu um Código Civil numa das prateleiras e decidiu levá-lo também. Saiu da loja com os livros nas mãos e uma sensação difícil de explicar. Para ele, aqueles livros representavam o primeiro passo de uma grande caminhada.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando chegou a casa, organizou cuidadosamente a sua mesa, afastou o telemóvel para não se distrair e colocou a Constituição à sua frente. Ficou alguns segundos a olhar para a capa antes de abrir o livro.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'É aqui que tudo começa.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Começou a ler com entusiasmo. Nas primeiras páginas sentiu que estava no caminho certo. As palavras pareciam importantes, os artigos pareciam conter respostas para todas as perguntas e ele acreditava que bastava continuar a estudar daquela forma para, pouco a pouco, dominar o Direito.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mas, depois de algum tempo, começou a sentir uma dificuldade que não esperava. João conseguia ler as palavras, entendia cada frase separadamente, mas quando terminava um artigo sentiu que algo estava a faltar. Era como se tivesse percorrido um caminho inteiro sem saber exatamente para onde estava a ir. Voltava ao início, relia o mesmo trecho, procurava uma explicação diferente, mas a sensação permanecia.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Como é possível eu conseguir ler isto e mesmo assim não entender?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Essa pergunta começou a incomodá-lo. Nos dias seguintes continuou a estudar. Sentava-se à mesa durante horas, sublinhava partes importantes, escrevia pequenas notas e tentava memorizar aquilo que considerava essencial. Quando um artigo mencionava outro, voltava atrás para ler também. Quando encontrava uma palavra desconhecida, procurava o significado. Ele estava a esforçar-se. Mas, quanto mais lia, mais percebia que existia uma distância entre simplesmente ler uma lei e realmente compreender aquilo que ela queria dizer.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Depois de alguns dias decidiu abrir o Código Civil, pensando que talvez encontrasse ali uma explicação mais fácil. Mas aconteceu exatamente o contrário. O número de dúvidas aumentou e aquela confiança que tinha no início começou lentamente a desaparecer.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Numa noite, depois de várias horas de estudo, João fechou o livro e ficou sentado em silêncio. Olhou para a Constituição sobre a mesa. Olhou para os outros livros ao lado. E, pela primeira vez desde que entrou na faculdade, teve um pensamento que o assustou.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'E se eu tiver escolhido o curso errado?'
      },
      {
        tipo: 'narrativa',
        texto:
          'A ideia parecia absurda, mas naquele momento parecia real. João sempre gostou de estudar. Durante o ensino médio, quando encontrava uma dificuldade, sabia que bastava dedicar mais tempo e acabaria por compreender. Nunca tinha sentido que uma matéria estivesse tão distante dele. Agora era diferente. Ele queria muito aprender, mas parecia que o Direito falava uma língua que ele ainda não conhecia.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Durante alguns dias deixou os livros fechados sobre a mesa. Não porque tivesse deixado de sonhar em ser magistrado, mas porque começou a duvidar se era realmente capaz de alcançar esse sonho.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Talvez existam pessoas que nasceram para isto e eu não seja uma delas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Esse pensamento passou várias vezes pela sua cabeça. Até que, numa manhã, durante uma aula na faculdade, um professor entrou na sala segurando apenas um código. Depois de explicar alguns conceitos, parou por alguns segundos e fez uma pergunta aos estudantes.'
      },
      {
        tipo: 'dialogo',
        quem: 'Professor',
        texto:
          'Digam-me uma coisa, vocês acham que um bom jurista é alguém que sabe todas as leis de memória?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Alguns estudantes responderam que sim. João também pensava dessa forma. O professor sorriu e respondeu:'
      },
      {
        tipo: 'dialogo',
        quem: 'Professor',
        texto:
          'Se fosse assim, bastava entregar um código a qualquer pessoa e ela tornar-se-ia jurista. O Direito não é decorar páginas. Um bom profissional precisa compreender o significado das normas, saber relacioná-las e perceber como aplicá-las aos problemas da vida real.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João ficou em silêncio. Aquelas palavras ficaram na sua cabeça durante toda a aula. Pela primeira vez começou a questionar uma ideia que sempre tinha considerado certa. Talvez o seu objetivo não fosse guardar milhares de artigos na memória. Talvez o verdadeiro desafio fosse aprender a compreender aquilo que estava por trás das palavras.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando chegou a casa, voltou a sentar-se diante da Constituição. Pegou no livro, mas desta vez não teve pressa em abrir. Ficou apenas a olhar para ele. Pensou em todos os dias que tinha passado a tentar decorar artigos, em todas as vezes que releu uma página sem entender e em toda a frustração que sentiu por acreditar que o problema estava nele.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mas talvez não estivesse. Talvez João simplesmente tivesse começado pelo lugar errado. Talvez antes de aprender as leis precisasse primeiro de aprender como o Direito funciona, como os juristas pensam e como transformar palavras escritas numa compreensão verdadeira.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Naquele momento ainda não sabia exatamente como faria isso, mas uma coisa tinha mudado. Ele já não olhava para os livros com medo. Agora olhava para eles com curiosidade. Porque tinha descoberto algo que iria acompanhá-lo durante toda a sua caminhada: estudar Direito não começa por decorar leis. Começa por aprender a compreendê-las. E, sem perceber, João tinha acabado de dar o primeiro passo para se tornar aquilo que sempre sonhou ser.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'joao-m1-a2',
    personagemId: 'joao',
    slug: 'o-erro-de-tentar-aprender-tudo',
    titulo: 'O erro de tentar aprender tudo ao mesmo tempo',
    modulo: 1,
    aula: 2,
    ordem: 2,
    fase: 'iniciante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Algumas mudanças importantes na vida não acontecem no momento em que encontramos uma resposta, mas no momento em que fazemos a pergunta certa.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João ainda não sabia como estudar Direito, mas uma coisa tinha mudado. Pela primeira vez desde que entrou na faculdade, deixou de acreditar que o problema era a sua capacidade. Agora sabia que precisava encontrar um novo caminho.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Nos dias seguintes voltou aos livros, mas desta vez tentou fazer algo diferente. Em vez de simplesmente abrir a Constituição e começar a ler, decidiu observar a forma como estudava. Percebeu algo que nunca tinha reparado antes. Ele queria aprender tudo ao mesmo tempo. Queria conhecer a Constituição, dominar o Código Civil, compreender os tribunais e entender como os magistrados pensavam. Tinha tanta vontade de chegar ao futuro que imaginava que estudar uma coisa de cada vez parecia pouco.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Se eu quero ser magistrado, preciso saber tudo.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Era assim que pensava. Por isso, numa manhã, abriu vários livros sobre a mesa. A Constituição estava no centro. Ao lado estava o Código Civil. Depois vieram alguns apontamentos da faculdade e um livro de introdução ao Direito. Olhou para aquela pilha e sentiu novamente aquela motivação dos primeiros dias.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Agora sim, vou conseguir avançar.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Começou pela Constituição. Depois passou para o Código Civil. Alguns minutos depois voltou aos apontamentos. Quando deu por si, tinha lido várias páginas, mas não conseguia explicar claramente o que tinha aprendido. Era uma sensação estranha. Tinha estudado muito, mas parecia que nada tinha ficado realmente organizado na sua cabeça.'
      },
      {
        tipo: 'narrativa',
        texto: 'Naquele momento lembrou-se de uma conversa que teve com um colega.'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'João, tu estás a tentar aprender Direito como se fosse uma corrida.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'E não é?'
      },
      {
        tipo: 'narrativa',
        texto: 'O colega pensou por alguns segundos antes de responder.'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'Acho que não. Talvez seja mais parecido com construir uma casa.'
      },
      {
        tipo: 'narrativa',
        texto:
          'A frase ficou na sua cabeça. Uma casa. João começou a imaginar. Ninguém começa uma casa pelo telhado. Primeiro vem a base, depois as paredes, depois as outras partes. Se alguém tentar colocar o telhado antes de construir a estrutura, tudo acaba por cair. Talvez com o Direito acontecesse a mesma coisa. Ele estava a tentar memorizar leis sem antes compreender os fundamentos. Estava a tentar entender respostas sem conhecer as perguntas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Naquela noite, João abriu o seu caderno e escreveu uma frase no topo da primeira página:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Antes de aprender muitas coisas, preciso aprender bem as primeiras coisas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Foi uma frase simples. Mas naquele momento representava uma mudança enorme. Durante muito tempo João pensou que avançar era ler mais páginas, terminar mais capítulos e acumular mais informação. Agora começava a perceber que avançar também podia significar parar, organizar e construir uma base mais forte.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Existe uma diferença entre quem apenas acumula conhecimento e quem realmente aprende. O primeiro preocupa-se com a quantidade de coisas que consegue guardar. O segundo preocupa-se com a forma como essas coisas se ligam.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João ainda não sabia, mas aquela mudança de pensamento seria uma das mais importantes da sua caminhada. Porque, no Direito, não vence aquele que corre mais depressa. Vence aquele que consegue construir um caminho que consegue sustentar.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'joao-m1-a3',
    personagemId: 'joao',
    slug: 'saber-ou-compreender',
    titulo: 'Saber a resposta ou compreender a resposta',
    modulo: 1,
    aula: 3,
    ordem: 3,
    fase: 'iniciante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Depois daquele dia, João começou a estudar de uma forma diferente. Ainda tinha muito para aprender, mas já não abria os livros com a mesma pressa dos primeiros dias. Antes de começar a ler, tentava perceber qual era o objetivo daquele estudo.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mesmo assim, uma nova dificuldade apareceu. João começou a perceber que, às vezes, achava que tinha entendido alguma coisa, mas bastava alguém fazer uma pergunta diferente para toda a sua segurança desaparecer.'
      },
      {
        tipo: 'narrativa',
        texto:
          'A primeira vez que reparou nisso foi durante uma conversa com um colega da faculdade. Estavam sentados no intervalo, a rever alguns apontamentos, quando o colega perguntou:'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'João, consegues explicar o que significa este conceito?'
      },
      {
        tipo: 'narrativa',
        texto: 'João olhou para a página e sorriu.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Sim, claro.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Tinha lido aquela definição várias vezes. Até tinha sublinhado a parte mais importante. Começou a responder usando praticamente as mesmas palavras do livro. O colega ouviu em silêncio e depois perguntou:'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'Mas explica com as tuas palavras. O que é que isso quer dizer na prática?'
      },
      {
        tipo: 'narrativa',
        texto:
          'João parou. Naquele momento percebeu que sabia repetir a frase, mas não sabia explicar a ideia. Era uma sensação estranha. A informação estava na sua cabeça, mas parecia presa dentro de uma caixa que ele não conseguia abrir.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando chegou a casa, voltou aos seus apontamentos. Começou a fazer uma pequena experiência. Pegou numa página que tinha estudado no dia anterior e tentou explicar o conteúdo sem olhar para o livro. No início parecia fácil. Mas depois começaram as dificuldades. Lembrava-se de algumas palavras importantes, mas não conseguia construir uma explicação completa.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Foi então que percebeu algo que o surpreendeu. Durante muitos anos, João tinha associado estudar a conseguir reconhecer uma informação. Se via uma frase conhecida, pensava:'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Eu sei isto.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mas agora começava a perceber que reconhecer não era o mesmo que compreender. Era como ouvir uma música várias vezes e conseguir cantar o refrão, mas não saber explicar o significado da letra. Ele conhecia as palavras, mas ainda não tinha entendido a mensagem.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Nos dias seguintes começou a mudar a forma como estudava. Quando lia um artigo, já não perguntava apenas: «Como posso decorar isto?» Começou a perguntar: «Como explicaria isto a alguém que nunca estudou Direito?»'
      },
      {
        tipo: 'narrativa',
        texto:
          'No início foi difícil. Algumas vezes ficava vários minutos parado, tentando encontrar uma forma simples de explicar uma ideia. Mas, pouco a pouco, algo começou a acontecer. Quanto mais tentava explicar com palavras simples, mais percebia aquilo que antes parecia confuso.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Um dia, enquanto estudava, João percebeu uma coisa curiosa. Os livros continuavam iguais. Os artigos continuavam escritos da mesma maneira. As palavras difíceis continuavam lá. Mas ele já não se sentia tão perdido como antes. A diferença não estava no livro. Estava na forma como ele estava a aproximar-se dele.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Há uma diferença silenciosa entre guardar uma informação e transformar essa informação em conhecimento. Muitas pessoas passam anos a estudar tentando encher a memória, mas poucas param para perguntar se realmente compreenderam aquilo que aprenderam.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João ainda estava no início da sua caminhada, mas começava a descobrir uma verdade importante: o Direito não pertence apenas a quem consegue repetir palavras difíceis. Pertence a quem consegue compreender ideias e dar-lhes sentido.'
      },
      {
        tipo: 'narrativa',
        texto: 'Naquela noite, antes de fechar o caderno, João escreveu uma nova frase:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Se eu não consigo explicar, talvez ainda não tenha entendido.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Ele não sabia naquele momento, mas aquela pequena mudança de pensamento acompanharia toda a sua formação. Porque um futuro magistrado não é alguém que apenas conhece respostas. É alguém que sabe compreender as perguntas.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'joao-m1-a4',
    personagemId: 'joao',
    slug: 'uma-lei-nao-vive-sozinha',
    titulo: 'O dia em que João descobriu que uma lei não vive sozinha',
    modulo: 1,
    aula: 4,
    ordem: 4,
    fase: 'iniciante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Depois de algumas semanas na faculdade, João começava a sentir que algo tinha mudado. Ainda havia muitos textos que pareciam difíceis, muitas palavras que precisava aprender e muitos assuntos que ainda não conseguia compreender completamente, mas uma coisa era diferente: já não sentia aquele medo dos primeiros dias.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Antes, quando abria um livro de Direito, sentia que estava diante de uma montanha impossível de subir. Agora percebia que o caminho era longo, mas pelo menos começava a entender como dar os primeiros passos.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mesmo assim, uma nova dificuldade apareceu. João começou a perceber que, muitas vezes, conseguia entender um artigo quando o lia sozinho, mas tudo voltava a ficar confuso quando tentava relacioná-lo com outros assuntos.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Foi numa aula de Introdução ao Direito que essa dificuldade ficou mais evidente. O professor estava a explicar um determinado tema e pediu aos estudantes que consultassem alguns artigos da Constituição. João abriu rapidamente o livro. Encontrou o artigo indicado e começou a ler.'
      },
      {
        tipo: 'narrativa',
        texto:
          'A primeira leitura pareceu simples. Ele entendeu algumas ideias principais, reconheceu algumas palavras que já tinha estudado e até sentiu uma pequena satisfação.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Finalmente estou a começar a entender.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mas alguns minutos depois o professor fez uma pergunta.'
      },
      {
        tipo: 'dialogo',
        quem: 'Professor',
        texto:
          'Alguém consegue explicar por que razão este artigo está ligado a outros princípios da Constituição?'
      },
      {
        tipo: 'narrativa',
        texto:
          'A sala ficou em silêncio. João olhou novamente para o artigo. Sabia onde ele estava. Sabia o que estava escrito. Mas não sabia responder. Naquele momento sentiu uma sensação que já conhecia. Era como se tivesse uma peça de um puzzle nas mãos, mas não soubesse onde encaixá-la.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando chegou a casa, decidiu voltar ao assunto. Abriu a Constituição e começou a fazer algo diferente. Em vez de apenas ler o artigo indicado, começou a procurar outros artigos relacionados. Percebeu que uma ideia aparecia em vários lugares, apresentada de formas diferentes. Um artigo explicava um princípio. Outro mostrava uma consequência. Outro apresentava uma proteção ou uma regra relacionada.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Pouco a pouco, começou a perceber uma coisa que nunca tinha pensado antes. As leis não eram frases separadas espalhadas num livro. Elas conversavam entre si. Era como uma grande rede onde cada parte tinha uma ligação com outra.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João lembrou-se dos primeiros dias na faculdade, quando tentava estudar como se cada artigo fosse uma informação independente que precisava guardar na memória. Agora percebia que aquele método estava a dificultar tudo. Ele não precisava apenas de perguntar: «O que este artigo diz?» Precisava também de perguntar: «Por que razão isto existe? Com que outras ideias isto está relacionado?»'
      },
      {
        tipo: 'narrativa',
        texto:
          'Essa pequena mudança alterou a forma como lia. Quando encontrava uma norma nova, já não passava imediatamente para a próxima página. Parava. Pensava. Tentava descobrir o motivo daquela regra existir.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Certo dia, enquanto estudava, percebeu algo que o deixou surpreendido. Ele estava a fazer exatamente aquilo que antes parecia impossível. Estava a conversar com o texto. Não literalmente, mas através de perguntas.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Por que o legislador escreveu isto desta forma?'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Qual problema esta regra tenta resolver?'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'O que poderia acontecer se esta norma não existisse?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Pela primeira vez, estudar Direito deixou de parecer apenas uma tarefa de memória. Começou a parecer uma investigação. E essa descoberta trouxe uma nova motivação.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Durante muito tempo, João acreditou que os livros jurídicos eram difíceis porque tinham muitas palavras complicadas. Agora começava a perceber que a verdadeira dificuldade estava em olhar para cada frase como se ela estivesse sozinha.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Uma lei nunca existe completamente isolada. Por trás de cada artigo existe uma razão, uma história, um objetivo e uma ligação com outros conhecimentos. Quem apenas lê palavras encontra textos. Quem procura compreender encontra sentido.'
      },
      {
        tipo: 'narrativa',
        texto: 'Naquela noite, João abriu o seu caderno e escreveu:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto:
          'Uma lei não é apenas aquilo que está escrito. É também aquilo que ela pretende proteger.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Ficou alguns segundos a olhar para aquela frase. Ainda tinha muito para aprender. Ainda estava longe de pensar como um verdadeiro jurista. Mas, pela primeira vez, sentiu que estava a começar a aproximar-se da forma de pensar que sempre procurou. E sem perceber, João já não estava apenas a estudar Direito. Estava a começar a entendê-lo.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'joao-m1-a5',
    personagemId: 'joao',
    slug: 'decorar-nao-era-aprender',
    titulo: 'O dia em que João percebeu que decorar não era aprender',
    modulo: 1,
    aula: 5,
    ordem: 5,
    fase: 'iniciante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Com o passar das semanas, João começou a sentir que estava diferente. Os livros continuavam grandes, os textos continuavam exigentes e ainda havia muitos assuntos que pareciam complicados, mas já não estudava da mesma forma que no início. Ele tinha aprendido a fazer perguntas, a procurar ligações entre as ideias e, principalmente, a não desistir no primeiro momento de dificuldade.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mesmo assim, havia uma coisa que ainda o incomodava. Nas avaliações da faculdade, João sentia que estudava bastante, mas os resultados nem sempre mostravam o esforço que fazia. Ele passava horas a ler. Fazia resumos. Sublinhava páginas inteiras. Escrevia definições no caderno. Às vezes conseguia repetir uma explicação quase palavra por palavra, mas, quando a pergunta vinha de uma forma diferente, sentia-se perdido.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Foi exatamente isso que aconteceu num pequeno teste da faculdade. João tinha estudado bastante para aquela prova. Nos dias anteriores, tinha revisto os seus apontamentos várias vezes e sentia-se preparado. Quando recebeu a folha com as perguntas, respirou fundo. A primeira pergunta parecia familiar. Ele já tinha visto aquele assunto. Começou a responder. Mas, depois de algumas linhas, percebeu que estava apenas a repetir aquilo que tinha lido no livro. A pergunta não pedia uma definição. Pedia que explicasse a ideia. E essa diferença mudou tudo.'
      },
      {
        tipo: 'narrativa',
        texto: 'João ficou alguns segundos parado, olhando para a folha.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Eu estudei isto... porque é que agora parece diferente?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando saiu da sala, ficou a pensar naquela situação. Ele não tinha esquecido completamente o conteúdo. O problema era outro. Ele tinha estudado para reconhecer a informação, mas não para usar aquela informação.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Durante os dias seguintes começou a observar melhor a sua forma de estudar. Percebeu que muitas vezes lia uma página e sentia satisfação apenas porque tinha terminado. Quanto mais páginas avançava, mais sentia que estava a progredir. Mas será que estava mesmo?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Uma noite, decidiu fazer uma experiência. Pegou num dos temas que tinha estudado e fechou todos os livros. Depois tentou explicar o assunto como se estivesse a conversar com alguém que nunca tinha ouvido falar sobre Direito. No início conseguiu explicar algumas partes. Depois começou a hesitar. Algumas ideias pareciam claras enquanto estavam escritas no livro, mas tornavam-se confusas quando precisava de colocá-las nas suas próprias palavras.'
      },
      {
        tipo: 'narrativa',
        texto: 'João sorriu ao perceber isso. Pela primeira vez, em vez de ficar frustrado, ficou curioso.'
      },
      {
        tipo: 'dialogo',
        quem: 'João',
        texto: 'Então é aqui que está a diferença.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Ele começou a perceber que estudar não era apenas colocar informação dentro da cabeça. Era conseguir retirar essa informação quando fosse necessário. Era conseguir pensar com ela.'
      },
      {
        tipo: 'narrativa',
        texto:
          'A partir daquele momento, mudou uma pequena parte da sua rotina. Depois de estudar um assunto, fechava o livro e perguntava a si mesmo: «Se alguém me perguntasse isto agora, eu conseguiria explicar?» Se a resposta fosse não, ele sabia que ainda precisava de voltar ao assunto.'
      },
      {
        tipo: 'narrativa',
        texto:
          'No início parecia mais lento. Antes conseguia ler dezenas de páginas num dia. Agora demorava mais tempo porque parava para pensar, tentava explicar e fazia perguntas. Mas havia uma diferença. Aquilo que aprendia permanecia mais tempo na sua memória. Pouco a pouco, João começou a perceber que o estudo mais rápido nem sempre era o estudo mais eficiente. Às vezes, parar para compreender era a forma mais rápida de avançar.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Existe uma armadilha comum no caminho de muitos estudantes. A sensação de aprender apenas porque uma informação parece familiar. Um texto pode parecer fácil porque já foi lido várias vezes, mas isso não significa que foi realmente compreendido. O verdadeiro conhecimento aparece quando conseguimos usar aquilo que aprendemos, explicar com clareza e aplicar numa situação diferente.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João ainda estava no início da sua caminhada, mas começava a descobrir uma diferença fundamental entre um estudante que apenas acumula informação e um estudante que desenvolve conhecimento. Um guarda respostas. O outro aprende a pensar.'
      },
      {
        tipo: 'narrativa',
        texto: 'Naquela noite, antes de dormir, João abriu o caderno e escreveu uma frase:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto:
          'Ler muitas vezes não significa aprender. Aprender é conseguir pensar com aquilo que li.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Fechou o caderno e apagou a luz. Ainda tinha muitos livros pela frente, muitos conceitos para compreender e muitos desafios para enfrentar. Mas agora sabia que cada dificuldade não era um sinal de que estava no caminho errado. Era apenas uma indicação de que estava a aprender algo novo. E essa mudança de pensamento, embora parecesse pequena, seria uma das maiores diferenças entre o João que entrou na faculdade e o magistrado que um dia sonhava tornar-se.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'joao-m1-a6',
    personagemId: 'joao',
    slug: 'direito-nao-e-opiniao',
    titulo: 'O dia em que João descobriu que Direito não era apenas uma questão de opinião',
    modulo: 1,
    aula: 6,
    ordem: 6,
    fase: 'iniciante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Com o passar do tempo, João começou a sentir-se mais confortável com a faculdade. Já não abria os livros com o mesmo medo dos primeiros dias, já não acreditava que precisava decorar tudo de uma vez e começava a perceber que estudar Direito era muito mais do que simplesmente ler artigos.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mas havia uma nova dificuldade à sua espera. João começou a perceber que, em muitas conversas, as pessoas tinham opiniões muito fortes sobre assuntos jurídicos. Às vezes, bastava alguém ouvir uma notícia ou assistir a uma discussão na televisão para imediatamente dizer o que achava certo ou errado. E João fazia o mesmo. Afinal, antes de entrar na faculdade, ele também via muitas situações apenas pelo lado do que parecia justo ou injusto.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Certa tarde, estava a conversar com alguns colegas depois da aula quando surgiu uma discussão sobre um caso que tinha passado nas notícias. Cada estudante começou a apresentar a sua opinião.'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'Para mim, isso está errado. A pessoa devia receber uma punição maior.'
      },
      {
        tipo: 'dialogo',
        quem: 'Outro colega',
        texto: 'Eu também penso assim.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João concordou imediatamente. A situação parecia simples. Na sua cabeça, já tinha uma resposta. Mas, no dia seguinte, o professor trouxe o mesmo assunto para a sala de aula. Só que, desta vez, não perguntou o que os estudantes achavam. Perguntou:'
      },
      {
        tipo: 'dialogo',
        quem: 'Professor',
        texto: 'Qual é o problema jurídico que precisamos analisar neste caso?'
      },
      {
        tipo: 'narrativa',
        texto:
          'A sala ficou em silêncio. João percebeu que aquela era uma pergunta diferente. O professor continuou:'
      },
      {
        tipo: 'dialogo',
        quem: 'Professor',
        texto:
          'No Direito, não basta perguntar se concordamos ou não com uma situação. Precisamos compreender quais são os factos, quais são as regras aplicáveis e quais são os argumentos que podem existir dos dois lados.'
      },
      {
        tipo: 'narrativa',
        texto:
          'João ficou a pensar nisso. Durante muito tempo acreditou que ser bom em Direito significava ter boas opiniões. Agora começava a perceber que era muito mais complicado. Uma pessoa podia ter uma opinião forte e, mesmo assim, não conseguir apresentar uma boa argumentação jurídica.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando chegou a casa, lembrou-se da conversa do dia anterior. Ele tinha dado a sua opinião rapidamente, mas não tinha feito algumas perguntas importantes. O que realmente aconteceu? Quais são os factos comprovados? Existe alguma regra que trate daquela situação? Essa regra aplica-se exatamente a este caso? Existem outros princípios que precisam ser considerados?'
      },
      {
        tipo: 'narrativa',
        texto:
          'João percebeu que tinha cometido um erro comum. Ele estava a olhar para o Direito apenas através das suas próprias ideias sobre o que parecia certo ou errado. Mas um jurista precisava de aprender a olhar para os problemas com mais cuidado. Não significava deixar de ter valores ou opiniões. Significava saber separar uma opinião pessoal de uma análise jurídica. Era uma diferença pequena, mas muito importante.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Nos dias seguintes começou a fazer uma pequena mudança. Sempre que ouvia uma notícia sobre um problema jurídico, tentava não responder imediatamente. Em vez disso, fazia perguntas. Tentava entender a situação antes de tirar uma conclusão. Às vezes descobria que a resposta não era tão simples como parecia no início. Outras vezes percebia que duas pessoas podiam defender posições diferentes usando argumentos que faziam sentido.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Isso deixou João ainda mais interessado pelo Direito. Ele começou a perceber que a beleza daquela área não estava apenas nas respostas. Estava também nas perguntas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Muitas pessoas procuram o Direito porque acreditam que ele serve apenas para dizer quem está certo e quem está errado. Com o tempo, os bons estudantes descobrem que o Direito é mais profundo. Ele exige atenção aos detalhes, capacidade de ouvir diferentes pontos de vista e disciplina para não decidir apenas com base na primeira impressão. Antes de procurar uma resposta, o jurista aprende a compreender o problema. Porque uma decisão justa não nasce apenas de uma boa intenção. Nasce de uma análise cuidadosa.'
      },
      {
        tipo: 'narrativa',
        texto: 'Naquela noite, João abriu o seu caderno e escreveu uma nova frase:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Antes de decidir quem tem razão, preciso primeiro compreender o problema.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Ficou alguns segundos a olhar para aquela frase. Era simples. Mas continha uma ideia que ele sabia que teria de levar consigo durante toda a sua caminhada. Porque um futuro magistrado não seria chamado apenas para dar respostas. Seria chamado para ouvir, analisar e decidir. E João começava, pouco a pouco, a preparar-se para essa responsabilidade.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  }
]
