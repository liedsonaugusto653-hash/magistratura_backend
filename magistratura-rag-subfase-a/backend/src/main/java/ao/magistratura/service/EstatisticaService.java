package ao.magistratura.service;

import ao.magistratura.dto.estatistica.EstatisticaResponse;
import ao.magistratura.dto.estatistica.EstatisticaResponse.SimuladoResumoEstatistica;
import ao.magistratura.entity.*;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Estatísticas de estudo — sempre calculadas a partir de dados reais da BD
 * (respostas, progresso de flashcards, histórico de estudo, tentativas).
 * Nunca devolve valores inventados ou hardcoded.
 */
@Service
@RequiredArgsConstructor
public class EstatisticaService {

    private final UtilizadorRepository utilizadorRepository;
    private final EstatisticaRepository estatisticaRepository;
    private final FlashcardProgressoRepository flashcardProgressoRepository;
    private final RespostaEstudanteRepository respostaEstudanteRepository;
    private final TentativaSimuladoRepository tentativaSimuladoRepository;
    private final HistoricoEstudoRepository historicoEstudoRepository;

    @Transactional(readOnly = true)
    public EstatisticaResponse obter(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador não encontrado"));

        java.util.UUID uid = utilizador.getId();

        // --- Horas e streak a partir do histórico real de estudo ---
        long segundos = historicoEstudoRepository.sumTempoSegundosByUtilizadorId(uid);
        double horasEstudo = Math.round((segundos / 3600.0) * 10.0) / 10.0;

        List<Instant> datasHist = historicoEstudoRepository.findDatasByUtilizadorId(uid);
        int diasConsecutivos = calcularDiasConsecutivos(datasHist);
        Instant ultimaAtividade = datasHist.isEmpty() ? null : datasHist.get(0);

        // Fallback: tabela estatisticas só se histórico estiver vazio (migração legada)
        if (segundos == 0 && ultimaAtividade == null) {
            Estatistica base = estatisticaRepository.findByUtilizadorId(uid).orElse(null);
            if (base != null) {
                if (base.getHorasEstudo() != null) {
                    horasEstudo = base.getHorasEstudo();
                }
                if (base.getDiasConsecutivos() != null) {
                    diasConsecutivos = base.getDiasConsecutivos();
                }
                if (base.getUltimaAtividade() != null) {
                    ultimaAtividade = base.getUltimaAtividade();
                }
            }
        }

        // --- Questões (resposta_estudante) ---
        long totalQuestoes = respostaEstudanteRepository.countByUtilizadorId(uid);
        long corretasQuestoes = respostaEstudanteRepository.countByUtilizadorIdAndCorretaTrue(uid);
        double percQuestoes = totalQuestoes == 0 ? 0.0
                : Math.round((corretasQuestoes * 1000.0) / totalQuestoes) / 10.0;

        // --- Flashcards (flashcard_progresso) ---
        List<FlashcardProgresso> progressos = flashcardProgressoRepository.findByUtilizadorId(uid);
        int fcConcluidos = progressos.size();
        int fcAcertos = progressos.stream().mapToInt(p -> p.getAcertos() != null ? p.getAcertos() : 0).sum();
        int fcErros = progressos.stream().mapToInt(p -> p.getErros() != null ? p.getErros() : 0).sum();
        int fcTotal = fcAcertos + fcErros;
        double percFlashcards = fcTotal == 0 ? 0.0
                : Math.round((fcAcertos * 1000.0) / fcTotal) / 10.0;

        // --- Simulados (se existirem tentativas; módulo pode estar desactivado na UI) ---
        List<TentativaSimulado> tentativas = tentativaSimuladoRepository
                .findByUtilizadorIdOrderByDataInicioDesc(uid)
                .stream()
                .filter(TentativaSimulado::isConcluido)
                .toList();

        int simuladosRealizados = tentativas.size();
        double mediaPontuacao = 0.0;
        double melhorPontuacao = 0.0;
        if (!tentativas.isEmpty()) {
            mediaPontuacao = tentativas.stream()
                    .mapToDouble(t -> t.getPontuacao() != null ? t.getPontuacao() : 0.0)
                    .average().orElse(0.0);
            mediaPontuacao = Math.round(mediaPontuacao * 10.0) / 10.0;
            melhorPontuacao = tentativas.stream()
                    .mapToDouble(t -> t.getPontuacao() != null ? t.getPontuacao() : 0.0)
                    .max().orElse(0.0);
            melhorPontuacao = Math.round(melhorPontuacao * 10.0) / 10.0;
        }

        List<SimuladoResumoEstatistica> historicoSim = tentativas.stream()
                .sorted(Comparator.comparing(TentativaSimulado::getDataInicio,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .map(t -> new SimuladoResumoEstatistica(
                        t.getDataFim() != null ? t.getDataFim() : t.getDataInicio(),
                        t.getSimulado() != null ? t.getSimulado().getTitulo() : null,
                        t.getPontuacao() != null ? t.getPontuacao() : 0.0
                ))
                .collect(Collectors.toList());

        // Actualizar ultimaAtividade se respostas/flashcards forem mais recentes (não temos timestamp fácil em todos)
        // Mantém o que veio do histórico de estudo.

        return new EstatisticaResponse(
                utilizador.getNome(),
                horasEstudo,
                diasConsecutivos,
                ultimaAtividade,
                (int) totalQuestoes,
                (int) corretasQuestoes,
                percQuestoes,
                fcConcluidos,
                fcAcertos,
                fcErros,
                percFlashcards,
                simuladosRealizados,
                mediaPontuacao,
                melhorPontuacao,
                historicoSim
        );
    }

    /**
     * Dias consecutivos de actividade até hoje (UTC), com base nas datas de historico_estudo.
     */
    static int calcularDiasConsecutivos(List<Instant> instants) {
        if (instants == null || instants.isEmpty()) {
            return 0;
        }
        Set<LocalDate> dias = new HashSet<>();
        for (Instant i : instants) {
            if (i != null) {
                dias.add(i.atZone(ZoneOffset.UTC).toLocalDate());
            }
        }
        if (dias.isEmpty()) {
            return 0;
        }
        LocalDate cursor = LocalDate.now(ZoneOffset.UTC);
        // Se não houve actividade hoje, começar a contar a partir de ontem se existir
        if (!dias.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!dias.contains(cursor)) {
                return 0;
            }
        }
        int streak = 0;
        while (dias.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
