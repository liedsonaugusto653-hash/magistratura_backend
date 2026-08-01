/**
 * Módulo 2 — Ana
 * Licenciada a preparar-se para o concurso da Magistratura.
 */

export const PERSONAGEM = {
  id: 'ana',
  nome: 'Ana',
  ocupacao: 'licenciada · candidata à Magistratura',
  resumo: 'Terminou a licenciatura e prepara o concurso. Quer acertar — e está a aprender o que isso realmente exige.'
}

export const MOMENTOS = [

  {
    id: 'ana-m2-a1',
    personagemId: 'ana',
    slug: 'o-mapa-antes-da-corrida',
    titulo: 'O mapa antes da corrida',
    modulo: 2,
    aula: 1,
    ordem: 1,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'Ana tinha terminado a licenciatura há poucos meses. O diploma estava na parede do quarto e, em cima da secretária, uma pilha de códigos e de provas de anos anteriores. O concurso da Magistratura deixara de ser um sonho longínquo: tinha data, tinha programa, tinha peso.'
      },
      {
        tipo: 'narrativa',
        texto: 'Nos primeiros dias de estudo sério, fez o que muita gente faz. Abriu um simulado antigo e tentou responder o mais depressa possível. Queria medir-se. Queria saber se já sabia o suficiente.'
      },
      {
        tipo: 'narrativa',
        texto: 'No fim da tarde, olhou para a folha. Tinha marcado muitas opções. Algumas por convicção. Outras por eliminação. Outras porque parecia a mais jurídica. Quando confrontou as soluções oficiais, o resultado não a destruiu — mas também não a deixou tranquila.'
      },
      {
        tipo: 'dialogo',
        quem: 'Ana',
        texto: 'Sei coisas. Mas não sei por que errei estas.'
      },
      {
        tipo: 'narrativa',
        texto: 'Na manhã seguinte, em vez de abrir outro simulado, fez uma lista. Não de artigos para decorar. De perguntas: O que é que o programa realmente exige? Em que matérias falhei por falta de base e em quais por pressa? O que preciso compreender antes de voltar a cronometrar?'
      },
      {
        tipo: 'narrativa',
        texto: 'Passou a manhã a reorganizar a mesa. Código Civil de um lado. Processo do outro. Um caderno só para dúvidas que ainda não sei explicar. Parecia menos heroico do que resolver cinquenta perguntas seguidas. Parecia, no entanto, mais honesto.'
      },
      {
        tipo: 'narrativa',
        texto: 'Há quem entre na preparação como quem entra numa corrida sem mapa. Corre muito. Cansa-se. E só depois repara que estava na estrada errada. Quem quer chegar longe aprende primeiro a olhar o terreno.'
      },
      {
        tipo: 'narrativa',
        texto: 'Antes de dormir, Ana escreveu no caderno:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Primeiro compreendo o caminho. Depois meço a velocidade.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a2',
    personagemId: 'ana',
    slug: 'factos-antes-do-julgamento',
    titulo: 'Factos antes do julgamento',
    modulo: 2,
    aula: 2,
    ordem: 2,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'Ana estava a treinar com um enunciado longo. Duas versões, um contrato, uma reclamação, datas espalhadas pelo texto. A tentação foi imediata — escolher um lado e procurar a norma que o confirmasse.'
      },
      {
        tipo: 'narrativa',
        texto: 'Parou a meio da primeira frase da resposta. Lembrou-se de uma conversa com um juiz num seminário aberto a candidatos.'
      },
      {
        tipo: 'dialogo',
        quem: 'Juiz',
        texto: 'Antes de perguntar quem tem razão, perguntem o que está provado. O resto vem depois — se vier.'
      },
      {
        tipo: 'narrativa',
        texto: 'Fechou o código. Numa folha em branco, fez três colunas: Factos do enunciado. O que o enunciado não diz. O que eu estou a assumir. A terceira coluna encheu-se depressa demais. Foi aí que percebeu o perigo.'
      },
      {
        tipo: 'dialogo',
        quem: 'Ana',
        texto: 'Estava a julgar com o que eu imagino, não com o que me deram.'
      },
      {
        tipo: 'narrativa',
        texto: 'Reescreveu a resposta. Começou pelos factos relevantes. Só depois pelas normas. No fim, a conclusão parecia menos brilhante e mais sólida. Menos opinião. Mais trabalho.'
      },
      {
        tipo: 'narrativa',
        texto: 'Um bom magistrado não decide porque a história o comoveu primeiro. Decide porque separou o que aconteceu do que se supõe — e só então aplicou o Direito.'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Primeiro o que está no processo. Depois a minha leitura. Nunca o contrário.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a3',
    personagemId: 'ana',
    slug: 'ouvir-os-dois-lados',
    titulo: 'Ouvir os dois lados',
    modulo: 2,
    aula: 3,
    ordem: 3,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'Num grupo de estudo, propuseram um caso polémico. Em cinco minutos, quase todos tinham uma posição. Ana também. Sentia-se segura — até alguém do outro lado da mesa apresentar um argumento que ela não tinha considerado.'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'E se a outra parte tiver razão nestes dois pontos? O que fazes à tua solução?'
      },
      {
        tipo: 'narrativa',
        texto: 'O silêncio que se seguiu não era constrangedor. Era útil. Ana reabriu o enunciado e forçou-se a escrever a melhor defesa possível da tese contrária. Não para ganhar o debate. Para testar a sua própria conclusão.'
      },
      {
        tipo: 'narrativa',
        texto: 'Quando voltou à sua resposta inicial, manteve parte dela. Alterou outra. Não porque tivesse mudado de opinião por pressão do grupo — porque a tese adversa tinha exposto uma fragilidade real.'
      },
      {
        tipo: 'narrativa',
        texto: 'No concurso, ninguém está lá para aplaudir a primeira frase que nos ocorre. No tribunal, ainda menos. Quem decide bem aprende a hospedar o argumento contrário dentro da própria cabeça antes de o rejeitar.'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Se não consigo enunciar a tese oposta com honestidade, ainda não estou pronta para decidir.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a4',
    personagemId: 'ana',
    slug: 'explicar-para-saber',
    titulo: 'Explicar para saber',
    modulo: 2,
    aula: 4,
    ordem: 4,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'Ana dominava, no papel, um tema de direitos fundamentais. Tinha esquemas. Tinha setas. Tinha cores. Quando a irmã, que não era da área, perguntou o que era reserva do possível, Ana começou uma frase longa e travou a meio.'
      },
      {
        tipo: 'dialogo',
        quem: 'Irmã',
        texto: 'Então… explica como se eu tivesse dez anos.'
      },
      {
        tipo: 'narrativa',
        texto: 'Riram. Depois Ana tentou de verdade. Duas frases claras. Um exemplo do quotidiano. Sem latim. Sem conforme a doutrina majoritária. Quando conseguiu, sentiu um alívio estranho — o mesmo que se sente ao pousar um peso que se carregava sem notar.'
      },
      {
        tipo: 'narrativa',
        texto: 'A partir daí, fechou o livro no fim de cada bloco e perguntou-se em voz alta se conseguiria explicar o essencial a alguém de fora. Onde falhava, voltava só ali. Não relia o capítulo inteiro por hábito.'
      },
      {
        tipo: 'narrativa',
        texto: 'Compreender não é reconhecer a página. É ser capaz de reconstruir a ideia quando a página não está à frente — e quando a pergunta vem torta.'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Se não explico simples, ainda não sei o suficiente para o concurso — nem para a vida.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a5',
    personagemId: 'ana',
    slug: 'a-rede-das-normas',
    titulo: 'A rede das normas',
    modulo: 2,
    aula: 5,
    ordem: 5,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'Num treino de constitucional, Ana isolou um artigo e respondeu com precisão. A correção, porém, apontava para um princípio que ela tinha deixado de fora — não por o desconhecer, mas por o tratar como outro tema.'
      },
      {
        tipo: 'dialogo',
        quem: 'Ana',
        texto: 'Eu sabia o artigo. Não sabia o sítio dele na casa.'
      },
      {
        tipo: 'narrativa',
        texto: 'Mudou o método. Ao estudar uma norma, passou a registar três linhas fixas: O que diz. Para que existe. Com o que se liga. Às vezes a terceira linha demorava mais do que a leitura do texto. Era tempo bem gasto.'
      },
      {
        tipo: 'narrativa',
        texto: 'Numa prova seguinte, a pergunta não pedia o artigo isolado. Pedia a articulação. Ana sentiu a diferença: já não caçava uma frase no índice mental. Percorria uma rede que ela própria tinha desenhado.'
      },
      {
        tipo: 'narrativa',
        texto: 'Uma lei raramente decide sozinha. Quem se prepara a sério estuda relações — não apenas fragmentos memorizáveis.'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Cada norma tem vizinhos. Estudar é também mapear a vizinhança.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a6',
    personagemId: 'ana',
    slug: 'errar-e-corrigir-o-rumo',
    titulo: 'Errar e corrigir o rumo',
    modulo: 2,
    aula: 6,
    ordem: 6,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'Ana errou uma questão que não podia errar. O tema era confortável. A pressa, não. Quando viu a solução, a primeira reacção foi irritação consigo mesma. A segunda, mais útil, foi abrir o caderno de erros.'
      },
      {
        tipo: 'narrativa',
        texto: 'Não escreveu só falhei X. Escreveu: O que li mal no enunciado. Que atalho mental tomei. Que regra ignorei. O que farei da próxima vez em perguntas parecidas. Quatro linhas. Sem drama.'
      },
      {
        tipo: 'dialogo',
        quem: 'Ana',
        texto: 'O erro só me atrasa se eu o esconder de mim.'
      },
      {
        tipo: 'narrativa',
        texto: 'Semanas depois, uma pergunta semelhante apareceu num simulado. Hesitou — e lembrou-se da nota. Respondeu com calma. Acertou. O prazer não foi o do acerto em si; foi o de ter usado o erro antigo como instrumento.'
      },
      {
        tipo: 'narrativa',
        texto: 'Um magistrado também se corrige: em recurso, em revisão de entendimento, na disciplina de admitir que a primeira leitura falhou. A preparação que esconde falhas treina vaidade. A que as examina treina ofício.'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Cada erro registado é uma aula que não preciso repetir no exame — nem no tribunal.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a7',
    personagemId: 'ana',
    slug: 'estrutura-sob-pressao',
    titulo: 'Estrutura sob pressão',
    modulo: 2,
    aula: 7,
    ordem: 7,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'O temporizador avançava. Ana tinha o hábito antigo de escrever tudo o que sabia sobre o tema, na esperança de que o corrector encontrasse a resposta no meio do texto. Dessa vez, parou aos dois minutos e fez um esqueleto: Questão jurídica. Factos relevantes. Normas. Aplicação. Conclusão.'
      },
      {
        tipo: 'narrativa',
        texto: 'Não preencheu o esqueleto com beleza. Preencheu com ordem. Quando o tempo acabou, faltavam detalhes — mas não faltava fio condutor. Na correcção, os pontos vinham precisamente dessa ordem.'
      },
      {
        tipo: 'dialogo',
        quem: 'Tutor de preparação',
        texto: 'No concurso, clareza é generosidade para quem lê. No tribunal, é respeito por quem vive a decisão.'
      },
      {
        tipo: 'narrativa',
        texto: 'Ana percebeu que a estrutura não era um luxo académico. Era uma forma de pensar sob pressão: obrigar a mente a escolher o que importa antes de encher a página.'
      },
      {
        tipo: 'narrativa',
        texto: 'Quem decide bem raramente começa pelo parágrafo final. Começa por saber o que está a decidir — e por quê.'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Sob pressão, a estrutura não me atrasa. Impede-me de me perder.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  },
  {
    id: 'ana-m2-a8',
    personagemId: 'ana',
    slug: 'a-pessoa-por-tras-do-caso',
    titulo: 'A pessoa por trás do caso',
    modulo: 2,
    aula: 8,
    ordem: 8,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto: 'No último simulado do mês, o enunciado era técnico e frio. Ana respondeu bem. Factos, normas, conclusão. Quando fechou a caneta, porém, ficou um resto de inquietação: tinha tratado o caso como um exercício — e era isso que ele era, naquele momento — mas um dia o papel teria nomes reais.'
      },
      {
        tipo: 'narrativa',
        texto: 'Não romantizou. Não inventou emoção onde o Direito pedia distância. Apenas acrescentou, no fim da correcção pessoal, uma pergunta:'
      },
      {
        tipo: 'dialogo',
        quem: 'Ana',
        texto: 'Se esta decisão afectasse a vida de alguém amanhã de manhã, eu ainda a escreveria assim?'
      },
      {
        tipo: 'narrativa',
        texto: 'A resposta foi sim, com um ajuste pequeno na fundamentação: mais cuidado na clareza, menos na exibição de erudição. A pessoa que ler a decisão merece entender o caminho, não apenas o resultado.'
      },
      {
        tipo: 'narrativa',
        texto: 'Preparar o concurso não é só acumular acertos. É ensaiar uma forma de estar perante o conflito: rigor sem indiferença, distância sem desprezo, coragem sem pressa.'
      },
      {
        tipo: 'narrativa',
        texto: 'Há quem estude para passar. Há quem estude para, um dia, decidir com o peso certo. Os dois objectivos podem caminhar juntos — se a preparação treinar a cabeça e, em silêncio, o carácter do ofício.'
      },
      {
        tipo: 'narrativa',
        texto: 'Ana fechou o caderno com uma frase só:'
      },
      {
        tipo: 'dialogo',
        quem: 'Caderno',
        texto: 'Por trás de cada questão de exame pode estar, um dia, a vida de alguém. Estudo também por isso.'
      },
      {
        tipo: 'narrativa',
        texto: 'À janela, a cidade seguia indiferente. Ana sorriu sem alarde. Ainda não era magistrada. Mas, pela primeira vez, sentiu que se preparava para o ser — e não apenas para um resultado num ranking.'
      },
    ],
    cta: { tipo: 'abrir_biblioteca', label: 'Abrir a Biblioteca', to: '/biblioteca' }
  }
]
