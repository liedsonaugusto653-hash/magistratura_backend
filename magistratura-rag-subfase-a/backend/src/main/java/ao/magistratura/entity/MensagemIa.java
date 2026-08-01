package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensagens_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemIa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private ConversaIa conversa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AutorMensagem autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diploma_contexto_id")
    private Diploma diplomaContexto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_contexto_id")
    private Artigo artigoContexto;

    /**
     * JSON array das fontes RAG citadas nesta resposta (só mensagens da IA).
     * Formato alinhado com {@code CitacaoFonteResponse}.
     */
    @Column(name = "fontes_json", columnDefinition = "TEXT")
    private String fontesJson;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
