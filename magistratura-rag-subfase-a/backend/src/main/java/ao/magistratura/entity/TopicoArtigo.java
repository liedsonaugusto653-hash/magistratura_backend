package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topico_artigo",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_topico_artigo",
                columnNames = {"topico_id", "artigo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicoArtigo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topico_id", nullable = false)
    private TopicoJuridico topico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artigo_id", nullable = false)
    private Artigo artigo;

    @Column(nullable = false)
    @Builder.Default
    private Float relevancia = 1.0f;

    /** MANUAL | IA | IMPORT */
    @Column(name = "origem_ligacao", nullable = false, length = 30)
    @Builder.Default
    private String origemLigacao = "MANUAL";

    @Column(name = "data_criacao", nullable = false)
    @Builder.Default
    private Instant dataCriacao = Instant.now();
}
