package ao.magistratura.dto.estatistica;

import java.time.Instant;
import java.util.List;

/**
 * Consolidação de progresso calculada a partir dos dados reais:
 * FlashcardProgresso, RespostaEstudante, TentativaSimulado, HistoricoEstudo, Estatistica.
 */
public record EstatisticaResponse(
        String nome,
        // --- Globais ---
        double horasEstudo,
        int diasConsecutivos,
        Instant ultimaAtividade,
        // --- Questões ---
        int questoesRespondidas,
        int questoesCorretas,
        double percentagemSucessoQuestoes,
        // --- Flashcards ---
        int flashcardsConcluidos,
        int flashcardsAcertos,
        int flashcardsErros,
        double percentagemSucessoFlashcards,
        // --- Simulados ---
        int simuladosRealizados,
        double mediaPontuacaoSimulados,
        double melhorPontuacaoSimulado,
        // --- Evolução recente ---
        List<SimuladoResumoEstatistica> historicoSimulados
) {
    public record SimuladoResumoEstatistica(
            Instant data,
            String titulo,
            double pontuacao
    ) {}
}
