package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa um ficheiro PDF importado para a Biblioteca Jurídica.
 * <p>
 * Um {@link Diploma} pode ter vários Documentos associados ao longo do tempo
 * (versão original, versão consolidada, edição comentada, correção, etc.).
 * {@code diploma} é nulo enquanto o documento está a ser importado ou
 * processado — só é obrigatório, por regra de negócio no {@code DocumentoService},
 * a partir do momento em que o estado passa a {@link EstadoDocumento#PROCESSADO}.
 */
@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diploma_id")
    private Diploma diploma;

    @Column(nullable = false)
    @Builder.Default
    private Integer versao = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoDocumento estado = EstadoDocumento.IMPORTADO;

    /** Origem do documento, texto livre (ex.: "Diário da República", "INEJ"). */
    @Column(length = 120)
    private String fonte;

    /** Distingue legislação oficial de material de apoio (manuais, notas, doutrina). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean oficial = true;

    @Column(name = "data_publicacao")
    private LocalDate dataPublicacao;

    @CreationTimestamp
    @Column(name = "data_importacao", nullable = false, updatable = false)
    private Instant dataImportacao;

    @Column(name = "hash_ficheiro", nullable = false, unique = true, length = 64)
    private String hashFicheiro;

    @Column(name = "numero_paginas")
    private Integer numeroPaginas;

    @Column(name = "caminho_ficheiro", nullable = false, length = 500)
    private String caminhoFicheiro;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    /** true quando a validação por contagem de artigos detetou um desvio relevante. */
    @Column(name = "revisao_necessaria", nullable = false)
    @Builder.Default
    private Boolean revisaoNecessaria = false;

    @Column(name = "observacoes_processamento", columnDefinition = "TEXT")
    private String observacoesProcessamento;

    @UpdateTimestamp
    @Column(name = "data_modificacao")
    private Instant dataModificacao;

    /** Etapa granular do pipeline (Fase 3). Independente de {@link #estado}. */
    @Column(name = "pipeline_etapa", length = 40)
    private String pipelineEtapa;

    /** Versão do pipeline que processou este documento. */
    @Column(name = "pipeline_versao", length = 20)
    private String pipelineVersao;

    /** Última etapa concluída com sucesso (reexecução parcial). */
    @Column(name = "ultima_etapa_ok", length = 40)
    private String ultimaEtapaOk;

    /** Método usado na última extracção de texto: PDFBOX, OCR_TESSERACT, HIBRIDO, NENHUM. */
    @Column(name = "metodo_extracao", length = 30)
    private String metodoExtracao;

    /** Confiança heurística 0–100 da última extracção. */
    @Column(name = "confianca_extracao")
    private Integer confiancaExtracao;

    /** Tipo de PDF classificado: TEXT, IMAGE, HYBRID, PROTECTED, UNKNOWN. */
    @Column(name = "tipo_pdf", length = 20)
    private String tipoPdf;

    @Column(name = "progresso_paginas_ok")
    private Integer progressoPaginasOk;

    @Column(name = "progresso_paginas_total")
    private Integer progressoPaginasTotal;

    @Column(name = "progresso_percentagem")
    private Integer progressoPercentagem;

    /** Mensagem curta orientada ao utilizador (sem jargão de pipeline). */
    @Column(name = "mensagem_progresso", length = 200)
    private String mensagemProgresso;

}
