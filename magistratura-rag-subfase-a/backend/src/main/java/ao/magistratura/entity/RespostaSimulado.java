package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "respostas_simulado", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tentativa_id", "questao_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaSimulado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tentativa_id", nullable = false)
    private TentativaSimulado tentativa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Enumerated(EnumType.STRING)
    @Column(name = "resposta_escolhida", length = 1)
    private OpcaoResposta respostaEscolhida;

    @Column(nullable = false)
    private boolean correta;
}
