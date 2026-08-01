package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "simulado_questoes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"simulado_id", "questao_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimuladoQuestao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_id", nullable = false)
    private Simulado simulado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(nullable = false)
    private Integer ordem;
}
