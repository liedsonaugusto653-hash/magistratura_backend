package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tentativas_simulado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TentativaSimulado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_id", nullable = false)
    private Simulado simulado;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    @Column(name = "pontuacao")
    private Double pontuacao;

    @Column(name = "concluido", nullable = false)
    @Builder.Default
    private boolean concluido = false;

    @OneToMany(mappedBy = "tentativa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RespostaSimulado> respostas = new java.util.ArrayList<>();
}
