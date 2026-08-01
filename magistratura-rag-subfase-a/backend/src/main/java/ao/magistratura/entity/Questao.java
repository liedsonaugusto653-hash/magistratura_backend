package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(name = "opcao_a", nullable = false, columnDefinition = "TEXT")
    private String opcaoA;

    @Column(name = "opcao_b", nullable = false, columnDefinition = "TEXT")
    private String opcaoB;

    @Column(name = "opcao_c", nullable = false, columnDefinition = "TEXT")
    private String opcaoC;

    @Column(name = "opcao_d", nullable = false, columnDefinition = "TEXT")
    private String opcaoD;

    @Enumerated(EnumType.STRING)
    @Column(name = "resposta_correta", nullable = false, length = 1)
    private OpcaoResposta respostaCorreta;

    @Column(columnDefinition = "TEXT")
    private String justificacao;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificuldade", nullable = false, length = 10)
    @Builder.Default
    private NivelDificuldade nivelDificuldade = NivelDificuldade.MEDIO;

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
    private UUID knowledgeOriginId;

    @Column(name = "generation_status", length = 20)
    private String generationStatus;
}
