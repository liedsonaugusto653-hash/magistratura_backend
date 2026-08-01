package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pergunta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id")
    private Tema tema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diploma_id")
    private Diploma diploma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id")
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private Documento documento;

    @Column(name = "pipeline_version", length = 20)
    private String pipelineVersion;

    @Column(name = "ai_provider", length = 80)
    private String aiProvider;

    @Column(name = "ai_model", length = 120)
    private String aiModel;

    @Column(name = "prompt_version", length = 40)
    private String promptVersion;

    @Column(name = "gerado_em")
    private Instant geradoEm;

    @Column(name = "estado_validacao", length = 20)
    private String estadoValidacao;

    @Column(name = "knowledge_origin_id")
    private java.util.UUID knowledgeOriginId;

    @Column(name = "generation_status", length = 20)
    private String generationStatus;
}
