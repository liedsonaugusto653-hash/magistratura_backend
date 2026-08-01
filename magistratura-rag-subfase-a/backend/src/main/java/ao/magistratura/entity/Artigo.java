package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "artigos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artigo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diploma_id", nullable = false)
    private Diploma diploma;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(length = 300)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private Integer ordem;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id")
    private Tema tema;

    @Column(name = "pagina_inicio")
    private Integer paginaInicio;

    @Column(name = "pagina_fim")
    private Integer paginaFim;

    /** Documento (PDF) de onde este artigo foi extraído. Nulo para artigos sem origem documental. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private Documento documento;

    @Column(length = 200)
    private String capitulo;

    @Column(length = 200)
    private String seccao;

    /** SHA-256 do conteúdo normalizado (número|título|texto) — diff incremental fino. */
    @Column(name = "hash_conteudo", length = 64)
    private String hashConteudo;
}
