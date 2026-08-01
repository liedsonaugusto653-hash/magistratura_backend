package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "estatisticas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estatistica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false, unique = true)
    private Utilizador utilizador;

    @Column(name = "horas_estudo", nullable = false)
    @Builder.Default
    private Double horasEstudo = 0.0;

    @Column(name = "dias_consecutivos", nullable = false)
    @Builder.Default
    private Integer diasConsecutivos = 0;

    @Column(name = "questoes_respondidas", nullable = false)
    @Builder.Default
    private Integer questoesRespondidas = 0;

    @Column(name = "questoes_corretas", nullable = false)
    @Builder.Default
    private Integer questoesCorretas = 0;

    @Column(name = "flashcards_concluidos", nullable = false)
    @Builder.Default
    private Integer flashcardsConcluidos = 0;

    @Column(name = "percentagem_sucesso", nullable = false)
    @Builder.Default
    private Double percentagemSucesso = 0.0;

    @Column(name = "ultima_atividade")
    private Instant ultimaAtividade;
}
