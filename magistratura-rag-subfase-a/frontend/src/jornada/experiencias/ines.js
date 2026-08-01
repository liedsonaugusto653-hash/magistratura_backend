/**
 * Inês — um momento denso.
 * Advogada júnior. A narrativa demasiado limpa.
 */

export const PERSONAGEM = {
  id: 'ines',
  nome: 'Inês',
  ocupacao: 'advogada júnior',
  resumo: 'Primeiro ano de prática. Quer fazer bem. Às vezes a história chega demasiado redonda.'
}

/** @type {Array<Object>} */
export const MOMENTOS = [
  {
    id: 'ines-01',
    personagemId: 'ines',
    slug: 'a-cliente-e-a-historia',
    titulo: 'A cliente e a história',
    ordem: 8,
    fase: 'outra',
    requerDiplomaProcessado: false,
    historia: [
      {
        tipo: 'narrativa',
        texto:
          'Inês recebeu o processo de uma cliente que alegava ter sido despedida por ter pedido licença de maternidade. A narrativa era clara, emocional, coerente. Inês acreditou de imediato.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Durante dois dias organizou documentos, sublinhou passagens, preparou argumentação. Cada e-mail da empresa parecia confirmar má-fé. Cada silêncio parecia prova.'
      },
      {
        tipo: 'dialogo',
        quem: 'Sócio',
        texto:
          'Olha também o registo de faltas e as avaliações dos últimos meses. Antes de fechares a tese.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Havia padrões anteriores. Avisos. Uma conversa disciplinar que a cliente não tinha mencionado. Inês sentiu um nó no estômago. Não era mentira pura. Era uma história que ela própria tinha deixado ficar demasiado limpa.'
      },
      {
        tipo: 'narrativa',
        texto:
          'Ficou até tarde no escritório. Não conseguia decidir se confrontava a cliente no dia seguinte ou se pedia só mais documentos. Fechou o computador sem ter escolhido. A luz do corredor ficou acesa.'
      }
    ],
    cta: {
      tipo: 'abrir_tutor',
      label: 'Abrir o Tutor',
      prefill: 'Como organizar factos quando a narrativa da cliente parece completa demais?',
      ancoraLogica: 'trabalho'
    }
  }
]
