package ao.magistratura.pipeline.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(nullable = false, length = 40)
    private String etapa;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    @Column(name = "duracao_ms")
    private Long duracaoMs;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "pipeline_versao", nullable = false, length = 20)
    private String pipelineVersao;

    @Column(name = "modelo_ia", length = 120)
    private String modeloIa;

    @Column(name = "ai_provider", length = 80)
    private String aiProvider;

    @Column(name = "prompt_version", length = 40)
    private String promptVersion;

    @Column(columnDefinition = "TEXT")
    private String detalhe;

    @Column(name = "hash_documento", length = 64)
    private String hashDocumento;

    @Column(name = "artigos_extraidos")
    private Integer artigosExtraidos;

    @Column(name = "num_erros")
    @Builder.Default
    private Integer numErros = 0;

    @Column(name = "num_avisos")
    @Builder.Default
    private Integer numAvisos = 0;

    @Column(name = "stacktrace_resumo", columnDefinition = "TEXT")
    private String stacktraceResumo;
}
