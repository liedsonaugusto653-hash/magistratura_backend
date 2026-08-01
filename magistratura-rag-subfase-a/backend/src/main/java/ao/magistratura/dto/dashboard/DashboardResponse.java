package ao.magistratura.dto.dashboard;

import java.time.Instant;
import java.util.List;

/**
 * Resumo do painel. Metricas = mesmas de EstatisticaResponse (EstatisticaService).
 */
public record DashboardResponse(
        String nome,
        double horasEstudo,
        int diasConsecutivos,
        int questoesRespondidas,
        int questoesCorretas,
        double percentagemSucessoQuestoes,
        double percentagemSucesso,
        int flashcardsConcluidos,
        double percentagemSucessoFlashcards,
        int simuladosRealizados,
        Instant ultimaAtividade,
        List<HistoricoItemResponse> historicoRecente
) {
    public record HistoricoItemResponse(
            Instant data,
            Integer tempoSegundos,
            String tituloArtigo,
            String tituloDiploma
    ) {}
}
