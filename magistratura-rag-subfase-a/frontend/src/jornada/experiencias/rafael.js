/**
 * O Caderno do Rafael — arco sustentado.
 * Espelho do estudante. Pessoas comuns. Sem moral explícita.
 * O caderno é o fio narrativo: cada momento é uma entrada.
 */

export const PERSONAGEM = {
  id: 'rafael',
  nome: 'Rafael',
  ocupacao: 'estudante de Direito',
  resumo: 'Candidato à magistratura. Anota o que o incomoda num caderno barato de capa preta.'
}

/** @type {Array<Object>} */
export const MOMENTOS = [
  {
    id: 'rafael-01',
    personagemId: 'rafael',
    slug: 'a-mensagem-no-grupo',
    titulo: 'A mensagem no grupo',
    ordem: 1,
    fase: 'estudante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Rafael estava no café da faculdade, o mesmo de sempre, com o caderno aberto na página das anotações de processo penal. O telemóvel vibrou duas vezes seguidas. No grupo de estudo, alguém tinha colado um print sem nome de autor.'
      },
      {
        tipo: 'dialogo',
        quem: 'Grupo',
        texto:
          'O professor X foi acusado de favorecer alunos. Já há queixa. Não se surpreendam se as notas saírem estranhas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Três reacções de indignação. Zero documentos. Rafael sentiu o corpo aquecer. Lembrava-se do teste de ontem — a pergunta ambígua, a resposta que ele tinha a certeza de estar certa, e o olhar do professor quando entregou a prova.'
      },
      {
        tipo: 'dialogo',
        quem: 'Rafael',
        texto: 'Viste? Faz sentido. Ele sempre foi estranho comigo.'
      },
      {
        tipo: 'dialogo',
        quem: 'Marta',
        texto: 'Ainda não há nada oficial. Talvez seja só rumor.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Rafael leu a resposta de Marta duas vezes. Fechou o telemóvel. Continuou a estudar, mas a concentração já não voltava. Cada vez que a pergunta do teste lhe vinha à cabeça, a hipótese do favorecimento parecia mais sólida.'
      },
      {
        tipo: 'narrativa',
        texto:
          'À noite, antes de dormir, abriu de novo o grupo. Havia mais mensagens. Nenhuma prova nova. Só mais gente a concordar. Ele não escreveu nada. No caderno, porém, riscou uma frase curta: «Confiar no que encaixa.» Depois sublinhou-a, sem saber bem porquê.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'rafael-02',
    personagemId: 'rafael',
    slug: 'o-numero-na-parede',
    titulo: 'O número na parede',
    ordem: 2,
    fase: 'estudante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'No corredor do departamento havia um quadro de cortiça com resultados de um simulado interno. Não era oficial. Alguém tinha escrito a lápis, no canto superior: «Média da turma: 12,4».'
      },
      {
        tipo: 'narrativa',
        texto:
          'Rafael tirou oito. Contou os pontos três vezes. O enunciado tinha uma alínea duvidosa; a grelha de correcção não esclarecia tudo. Mesmo assim, o 12,4 ficou-lhe na cabeça como se fosse a medida do mundo.'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'Estás a fazer cara de quem viu um fantasma.'
      },
      {
        tipo: 'dialogo',
        quem: 'Rafael',
        texto: 'A média foi alta. Eu fiquei longe.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Em casa, abriu o caderno. Queria escrever sobre a alínea duvidosa. Em vez disso, escreveu o número: 12,4. Depois o seu: 8. A diferença parecia maior do que a soma das partes. Fechou o caderno sem terminar a frase.'
      },
      {
        tipo: 'narrativa',
        texto:
          'No dia seguinte, um professor comentou que aquele quadro era só um rascunho de um grupo de estudo — a “média” não tinha base completa. Rafael ouviu. Acenou. O número, contudo, já tinha feito o seu trabalho.'
      }
    ],
    cta: {
      tipo: 'abrir_tutor',
      label: 'Perguntar ao Tutor',
      prefill: 'Como se interpreta uma nota quando a grelha de correcção é ambígua?',
      ancoraLogica: null
    }
  },
  {
    id: 'rafael-03',
    personagemId: 'rafael',
    slug: 'a-espera-na-esquadra',
    titulo: 'A espera na esquadra',
    ordem: 3,
    fase: 'estudante',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'O primo de Rafael tinha sido chamado para prestar declarações por uma briga no bairro. Nada grave, disseram. Rafael foi com ele porque a tia não podia faltar ao trabalho.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Na sala de espera, um agente passou depressa, olhou para o primo e murmurou para um colega algo que Rafael não percebeu inteiro. Só apanhou o tom. O primo encolheu os ombros, habituado.'
      },
      {
        tipo: 'dialogo',
        quem: 'Primo',
        texto: 'Relaxa. Isto é só formalidade.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Rafael não relaxou. Olhava para a forma como o primo falava, para o corte de cabelo, para a forma de se sentar. De repente, tudo nele parecia “encajar” num tipo de pessoa que Rafael só conhecia de notícias e de conversas de corredor.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando saíram, o sol batia forte no asfalto. O primo ria de uma piada má. Rafael riu também, atrasado. No caderno, essa noite, escreveu apenas: «Eu já tinha decidido antes de ouvir.» Riscou a frase. Voltou a escrevê-la por baixo, mais pequena.'
      }
    ]
  },
  {
    id: 'rafael-04',
    personagemId: 'rafael',
    slug: 'a-historia-que-encaixava',
    titulo: 'A história que encaixava',
    ordem: 4,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Uma colega de estágio contou a Rafael, no intervalo, que tinha sido afastada de um processo porque “não combinava com a equipa”. A narrativa era limpa: horários cumpridos, esforço, uma conversa fria no fim.'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'Eu só queria trabalhar. Eles é que não queriam alguém a fazer perguntas.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Rafael acreditou de imediato. Passou a tarde a imaginar o cenário. Cada detalhe que ela acrescentava encaixava. Ele já estava a construir a argumentação mental a favor dela — como se fosse um caso seu.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Dois dias depois, outro estagiário mencionou, de passagem, que havia faltas repetidas e um aviso formal. Rafael sentiu um nó no estômago. Não era que a colega tivesse mentido. Era que a história que ele tinha montado era demasiado redonda.'
      },
      {
        tipo: 'narrativa',
        texto:
          'No caderno, em vez de um resumo, deixou um espaço em branco com uma data. Não sabia o que escrever sem trair a complexidade. Fechou o caderno com o dedo ainda no meio das páginas.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Ir à Biblioteca',
      to: '/biblioteca'
    }
  },
  {
    id: 'rafael-05',
    personagemId: 'rafael',
    slug: 'a-pagina-que-nao-fechou',
    titulo: 'A página que não fechou',
    ordem: 5,
    fase: 'candidato',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Era quase uma da manhã. Rafael tinha o Código aberto ao lado do caderno. Queria reler um artigo sobre presunção de inocência — não para um trabalho, mas porque a frase do primo e a história da estagiária lhe voltavam sem pedido.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Lia em voz baixa. Parava. Voltava atrás. O texto parecia claro e, ao mesmo tempo, exigia mais do que ele estava disposto a admitir naquele momento.'
      },
      {
        tipo: 'dialogo',
        quem: 'Rafael',
        texto: 'Se eu já decido antes de ouvir… o que é que estou a praticar exactamente?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Não respondeu a si próprio. Sublinhou no caderno uma única linha do artigo, sem comentário. Deixou o lápis pousado em cima da página. A luz do quarto cortava o silêncio da rua.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Quando finalmente deitou, o caderno ficou aberto na secretária. A página não fechou. De manhã, antes de sair, passou o dedo pelo sublinhado outra vez — e foi estudar com a sensação de que ainda estava a meio de uma conversa consigo mesmo.'
      }
    ],
    cta: {
      tipo: 'abrir_artigo',
      label: 'Abrir na Biblioteca',
      ancoraLogica: 'direitos-fundamentais'
    }
  }
]
