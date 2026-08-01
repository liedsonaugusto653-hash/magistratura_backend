package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "relacoes_juridicas",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_relacao",
                columnNames = {"origem_id", "destino_id", "tipo_relacao"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelacaoJuridica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origem_id", nullable = false)
    private TopicoJuridico origem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_id", nullable = false)
    private TopicoJuridico destino;

    /** REGULADO_POR | PRESSUPOE | OPOE_SE | ESPECIALIZA | APLICA_SE_A | CONEXO */
    @Column(name = "tipo_relacao", nullable = false, length = 40)
    private String tipoRelacao;

    @Column(nullable = false)
    @Builder.Default
    private Float peso = 1.0f;

    @Column(columnDefinition = "TEXT")
    private String notas;
}
