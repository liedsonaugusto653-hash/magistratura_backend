/**
 * Tomás — um momento denso.
 * Agente da polícia. O perfil que decide antes da prova.
 */

export const PERSONAGEM = {
  id: 'tomas',
  nome: 'Tomás',
  ocupacao: 'agente da polícia',
  resumo: 'Turno da noite. Viu muita coisa. Às vezes reconhece padrões demais.'
}

/** @type {Array<Object>} */
export const MOMENTOS = [
  {
    id: 'tomas-01',
    personagemId: 'tomas',
    slug: 'a-cara-que-ja-conhecia',
    titulo: 'A cara que já conhecia',
    ordem: 7,
    fase: 'outra',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Tomás chegou a uma ocorrência de furto numa loja. O segurança apontou para um jovem sentado no chão, mãos visíveis.'
      },
      {
        tipo: 'dialogo',
        quem: 'Segurança',
        texto: 'Foi ele. Vi-o a mexer nas prateleiras.'
      },
      {
        tipo: 'dialogo',
        quem: 'Jovem',
        texto: 'Só estava a olhar. Não tenho nada.'
      },
      {
        tipo: 'narrativa',
        texto:
          'O dono da loja estava nervoso e insistia. Tomás olhou para o rapaz. Reconheceu o bairro. Reconheceu o tipo de corte de cabelo. Reconheceu a forma de falar. Já tinha visto “este perfil” noutros casos.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Enquanto fazia as perguntas de rotina, a voz saiu-lhe mais seca do que o habitual. Quando o jovem hesitou numa resposta, Tomás sentiu que a hesitação confirmava o que já sabia.'
      },
      {
        tipo: 'narrativa',
        texto:
          'No relatório, escreveu com objectividade. No caminho de casa, a imagem que lhe ficou não foi a das prateleiras. Foi a cara do rapaz — e a certeza de que “era mesmo ele”.'
      },
      {
        tipo: 'narrativa',
        texto:
          'No dia seguinte, um colega mandou mensagem: tinham apanhado o verdadeiro autor noutro local. Tomás leu em silêncio. Não respondeu. Guardou o telemóvel no bolso e ficou a olhar para o trânsito durante mais tempo do que o necessário.'
      }
    ]
  }
]
