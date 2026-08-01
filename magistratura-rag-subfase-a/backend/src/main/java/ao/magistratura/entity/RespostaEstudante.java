package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resposta_estudante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaEstudante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Enumerated(EnumType.STRING)
    @Column(name = "resposta_escolhida", nullable = false, length = 1)
    private OpcaoResposta respostaEscolhida;

    @Column(nullable = false)
    private boolean correta;

    @CreationTimestamp
    @Column(name = "data_resposta", nullable = false, updatable = false)
    private Instant dataResposta;
}
