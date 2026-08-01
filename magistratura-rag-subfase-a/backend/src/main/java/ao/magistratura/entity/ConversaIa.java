package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversas_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversaIa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @Column(nullable = false, length = 250)
    @Builder.Default
    private String titulo = "Nova conversa";

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("timestamp ASC")
    private List<MensagemIa> mensagens = new java.util.ArrayList<>();
}
