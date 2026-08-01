package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Preparação para RAG (Retrieval-Augmented Generation).
 * <p>
 * Mapeia a tabela {@code documento_embeddings}, já criada em V1__schema.sql.
 * O campo {@code vetor} usa {@code double[]} como armazenamento provisório;
 * quando a extensão pgvector for ativada no PostgreSQL, a coluna pode ser
 * migrada para o tipo {@code vector} sem alterar o resto da arquitetura —
 * apenas o mapeamento desta entidade precisa de ajuste.
 */
@Entity
@Table(name = "documento_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id")
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resumo_id")
    private Resumo resumo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vetor", columnDefinition = "double precision[]")
    private double[] vetor;

    @Column(name = "modelo_embedding", length = 100)
    private String modeloEmbedding;

    @Column(length = 200)
    private String origem;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;
}
