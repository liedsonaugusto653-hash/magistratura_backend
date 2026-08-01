package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilizadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "fotografia_url")
    private String fotografiaUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    /** Preferências de UI/estudo em JSON (tema, notificações, etc.). */
    @Column(name = "preferencias_json", columnDefinition = "TEXT")
    private String preferenciasJson;
}
