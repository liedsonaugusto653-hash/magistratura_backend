package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "resumos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resumo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diploma_id")
    private Diploma diploma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id")
    private Tema tema;
}
