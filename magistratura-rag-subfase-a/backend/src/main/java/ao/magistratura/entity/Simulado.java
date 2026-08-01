package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "simulados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simulado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "tempo_minutos", nullable = false)
    private Integer tempoMinutos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @OneToMany(mappedBy = "simulado", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SimuladoQuestao> perguntas = new java.util.ArrayList<>();
}
