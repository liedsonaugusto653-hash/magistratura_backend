package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilizador_id", nullable = false)
    private UUID utilizadorId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
}
