package ao.magistratura.pipeline.audit;

import ao.magistratura.pipeline.model.PipelineEtapa;
import ao.magistratura.pipeline.model.PipelineResultado;
import ao.magistratura.pipeline.model.PipelineVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PipelineAuditService {

    private final PipelineAuditoriaRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID iniciar(UUID documentoId, PipelineEtapa etapa, String hashDocumento) {
        PipelineAuditoria registo = PipelineAuditoria.builder()
                .documentoId(documentoId)
                .etapa(etapa.name())
                .dataInicio(Instant.now())
                .resultado(PipelineResultado.SUCESSO.name())
                .pipelineVersao(PipelineVersion.ATUAL)
                .hashDocumento(hashDocumento)
                .numErros(0)
                .numAvisos(0)
                .build();
        return repository.save(registo).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluir(UUID registoId, PipelineResultado resultado, String detalhe,
                         String mensagemErro, Integer artigosExtraidos, Integer numAvisos,
                         Throwable erro) {
        repository.findById(registoId).ifPresent(r -> {
            Instant fim = Instant.now();
            r.setDataFim(fim);
            r.setDuracaoMs(Duration.between(r.getDataInicio(), fim).toMillis());
            r.setResultado(resultado.name());
            r.setDetalhe(detalhe);
            r.setMensagemErro(mensagemErro);
            r.setArtigosExtraidos(artigosExtraidos);
            r.setNumAvisos(numAvisos != null ? numAvisos : 0);
            r.setNumErros(resultado == PipelineResultado.ERRO ? 1 : 0);
            if (erro != null) {
                r.setStacktraceResumo(resumirStack(erro));
            }
            repository.save(r);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarSimples(UUID documentoId, PipelineEtapa etapa, PipelineResultado resultado,
                                String detalhe, String hashDocumento, String mensagemErro) {
        registarSimples(documentoId, etapa, resultado, detalhe, hashDocumento, mensagemErro, null, 0, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarSimples(UUID documentoId, PipelineEtapa etapa, PipelineResultado resultado,
                                String detalhe, String hashDocumento, String mensagemErro,
                                Integer artigosExtraidos, int numAvisos, Throwable erro) {
        Instant agora = Instant.now();
        PipelineAuditoria registo = PipelineAuditoria.builder()
                .documentoId(documentoId)
                .etapa(etapa.name())
                .dataInicio(agora)
                .dataFim(agora)
                .duracaoMs(0L)
                .resultado(resultado.name())
                .pipelineVersao(PipelineVersion.ATUAL)
                .hashDocumento(hashDocumento)
                .detalhe(detalhe)
                .mensagemErro(mensagemErro)
                .artigosExtraidos(artigosExtraidos)
                .numAvisos(numAvisos)
                .numErros(resultado == PipelineResultado.ERRO ? 1 : 0)
                .stacktraceResumo(erro != null ? resumirStack(erro) : null)
                .build();
        repository.save(registo);
    }

    @Transactional(readOnly = true)
    public List<PipelineAuditoria> historico(UUID documentoId) {
        return repository.findByDocumentoIdOrderByDataInicioAsc(documentoId);
    }

    private static String resumirStack(Throwable erro) {
        StringWriter sw = new StringWriter();
        erro.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();
        return full.length() > 2000 ? full.substring(0, 2000) + "…" : full;
    }
}
