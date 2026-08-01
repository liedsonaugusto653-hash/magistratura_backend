package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entidades_juridicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntidadeJuridica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 40)
    private String icone;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "data_criacao", nullable = false)
    @Builder.Default
    private Instant dataCriacao = Instant.now();
}
