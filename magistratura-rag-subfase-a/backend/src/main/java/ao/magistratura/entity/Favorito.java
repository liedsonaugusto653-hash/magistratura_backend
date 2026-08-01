package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favoritos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id")
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diploma_id")
    private Diploma diploma;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;
}
