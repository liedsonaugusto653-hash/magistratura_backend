package ao.magistratura.knowledge.origin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Origem lógica estável do conhecimento.
 * <p>
 * Sobrevive ao reprocessamento: quando o {@code Artigo} JPA é apagado e recriado,
 * o {@code originKey} mantém-se e {@code artigoIdAtual} é atualizado.
 * Chave típica: {@code doc:{documentoId}|art:{numero}}.
 */
@Entity
@Table(name = "knowledge_origin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeOrigin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "origin_key", nullable = false, unique = true, length = 200)
    private String originKey;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "diploma_id")
    private UUID diplomaId;

    @Column(name = "artigo_numero", length = 30)
    private String artigoNumero;

    @Column(name = "artigo_hash", length = 64)
    private String artigoHash;

    @Column(name = "artigo_id_atual")
    private UUID artigoIdAtual;

    @Column(name = "pipeline_version", length = 20)
    private String pipelineVersion;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_atualizacao")
    private Instant dataAtualizacao;

    public static String keyFor(UUID documentoId, String artigoNumero) {
        return "doc:" + documentoId + "|art:" + (artigoNumero != null ? artigoNumero.trim() : "?");
    }
}
