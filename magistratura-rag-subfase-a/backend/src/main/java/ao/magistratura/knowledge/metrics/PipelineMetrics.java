package ao.magistratura.knowledge.metrics;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_metricas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "execucao_id")
    private UUID execucaoId;

    @Column(name = "pipeline_version", nullable = false, length = 20)
    private String pipelineVersion;

    @Column(name = "data_registo", nullable = false)
    private Instant dataRegisto;

    @Column(name = "documentos_processados")
    @Builder.Default
    private Integer documentosProcessados = 1;

    @Column(name = "artigos_extraidos")
    @Builder.Default
    private Integer artigosExtraidos = 0;

    @Column(name = "artigos_novos")
    @Builder.Default
    private Integer artigosNovos = 0;

    @Column(name = "artigos_alterados")
    @Builder.Default
    private Integer artigosAlterados = 0;

    @Column(name = "artigos_removidos")
    @Builder.Default
    private Integer artigosRemovidos = 0;

    @Column(name = "conhecimento_gerado")
    @Builder.Default
    private Integer conhecimentoGerado = 0;

    @Column(name = "falhas")
    @Builder.Default
    private Integer falhas = 0;

    @Column(name = "duracao_total_ms")
    private Long duracaoTotalMs;

    @Column(name = "detalhe_etapas", columnDefinition = "TEXT")
    private String detalheEtapas;
}
