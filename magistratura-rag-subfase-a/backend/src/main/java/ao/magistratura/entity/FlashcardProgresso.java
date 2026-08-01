package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_progresso",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_flashcard_progresso_utilizador_flashcard",
               columnNames = {"utilizador_id", "flashcard_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardProgresso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @Column(name = "vezes_revisto", nullable = false)
    @Builder.Default
    private Integer vezesRevisto = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer acertos = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer erros = 0;

    @Column(name = "nivel_dificuldade", nullable = false, length = 15)
    @Builder.Default
    private String nivelDificuldade = "MEDIO";

    @Column(name = "ultima_revisao")
    private Instant ultimaRevisao;

    @Column(name = "proxima_revisao")
    private Instant proximaRevisao;
}
