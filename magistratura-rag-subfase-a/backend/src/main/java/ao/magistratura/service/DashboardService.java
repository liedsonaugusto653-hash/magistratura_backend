package ao.magistratura.service;

import ao.magistratura.dto.dashboard.DashboardResponse;
import ao.magistratura.dto.dashboard.DashboardResponse.HistoricoItemResponse;
import ao.magistratura.dto.estatistica.EstatisticaResponse;
import ao.magistratura.entity.HistoricoEstudo;
import ao.magistratura.entity.Utilizador;
import ao.magistratura.exception.RecursoNaoEncontradoException;
import ao.magistratura.repository.HistoricoEstudoRepository;
import ao.magistratura.repository.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UtilizadorRepository utilizadorRepository;
    private final HistoricoEstudoRepository historicoEstudoRepository;
    private final EstatisticaService estatisticaService;

    @Transactional(readOnly = true)
    public DashboardResponse obterDashboard(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

        EstatisticaResponse s = estatisticaService.obter(email);

        List<HistoricoEstudo> historico = historicoEstudoRepository
                .findTop10ByUtilizadorIdOrderByDataDesc(utilizador.getId());

        List<HistoricoItemResponse> historicoDto = historico == null
                ? Collections.emptyList()
                : historico.stream()
                    .map(h -> new HistoricoItemResponse(
                            h.getData(),
                            h.getTempoSegundos(),
                            h.getArtigo() != null ? h.getArtigo().getTitulo() : null,
                            h.getDiploma() != null ? h.getDiploma().getTitulo() : null
                    ))
                    .toList();

        double percQ = s.percentagemSucessoQuestoes();

        return new DashboardResponse(
                s.nome() != null ? s.nome() : utilizador.getNome(),
                s.horasEstudo(),
                s.diasConsecutivos(),
                s.questoesRespondidas(),
                s.questoesCorretas(),
                percQ,
                percQ,
                s.flashcardsConcluidos(),
                s.percentagemSucessoFlashcards(),
                s.simuladosRealizados(),
                s.ultimaAtividade(),
                historicoDto
        );
    }
}
