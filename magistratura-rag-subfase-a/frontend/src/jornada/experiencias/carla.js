/**
 * Carla — um momento denso.
 * Assistente administrativa. O número que entra primeiro.
 */

export const PERSONAGEM = {
  id: 'carla',
  nome: 'Carla',
  ocupacao: 'assistente administrativa',
  resumo: 'Trabalha num tribunal. Mãe de dois. Anota tudo o que ouve nos corredores.'
}

/** @type {Array<Object>} */
export const MOMENTOS = [
  {
    id: 'carla-01',
    personagemId: 'carla',
    slug: 'o-numero-que-ficou',
    titulo: 'O número que ficou',
    ordem: 6,
    fase: 'outra',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Carla organizava pastas de contra-ordenação quando um colega passou no corredor e comentou, quase a rir:'
      },
      {
        tipo: 'dialogo',
        quem: 'Colega',
        texto: 'Naquele processo da Marginal, o pedido de indemnização ia nos cinco milhões. Absurdo.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Ela anotou mentalmente: cinco milhões. Mais tarde, ao abrir o processo para confirmar uma data, leu com atenção. O pedido formal era de 1,8 milhões. Havia facturas, laudos, números concretos.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Mesmo assim, enquanto redigia a nota interna, a frase que lhe vinha à cabeça era “o pedido elevado”. Não conseguia deixar de sentir que 1,8 já era “muito” — porque o cinco milhões tinha entrado primeiro.'
      },
      {
        tipo: 'dialogo',
        quem: 'Marido',
        texto: 'Como correu o dia?'
      },
      {
        tipo: 'dialogo',
        quem: 'Carla',
        texto: 'Há gente a pedir valores absurdos. Cinco milhões por um acidente…'
      },
      {
        tipo: 'dialogo',
        quem: 'Marido',
        texto: 'Era mesmo cinco?'
      },
      {
        tipo: 'narrativa',
        texto:
          'Carla hesitou. Depois respondeu: «Mais ou menos. Era alto.» Ficou em silêncio o resto do jantar. Antes de dormir, ainda via o número grande, nítido, como se estivesse escrito na parede do quarto.'
      }
    ],
    cta: {
      tipo: 'abrir_biblioteca',
      label: 'Abrir a Biblioteca',
      to: '/biblioteca'
    }
  }
]
