package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "diplomas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diploma {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String numero;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private LocalDate dataPublicacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoDiploma estado = EstadoDiploma.VIGENTE;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    /**
     * @deprecated o PDF passa a ser gerido através de {@link Documento}
     * (um Diploma pode ter vários Documentos). Mantido apenas por
     * compatibilidade com dados/leituras existentes; não é preenchido
     * pelo módulo de Biblioteca Jurídica a partir daqui.
     */
    @Deprecated
    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "versao", nullable = false)
    @Builder.Default
    private Integer versao = 1;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_modificacao")
    private Instant dataModificacao;
}
