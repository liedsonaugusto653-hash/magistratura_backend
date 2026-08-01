package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topicos_juridicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicoJuridico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidade_id")
    private EntidadeJuridica entidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TopicoJuridico parent;

    /** Ponte opcional para a taxonomia legada categorias/temas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    /** Definição curta do conceito, gerada por IA e cacheada (Ficha de Estudo). */
    @Column(name = "definicao_estudo", columnDefinition = "TEXT")
    private String definicaoEstudo;

    /** Perguntas-guia (JSON: [{"pergunta":"...","resposta":"..."}]), geradas por IA e cacheadas. */
    @Column(name = "perguntas_guia", columnDefinition = "TEXT")
    private String perguntasGuia;

    @Column(name = "perguntas_guia_gerado_em")
    private Instant perguntasGuiaGeradoEm;

    /** Porquê o conceito existe — o problema humano que lhe deu origem (gerado por IA, cacheado). */
    @Column(name = "porque_existe", columnDefinition = "TEXT")
    private String porqueExiste;

    /** Exemplos do quotidiano onde o conceito aparece (JSON: ["...", "..."]). */
    @Column(name = "onde_aparece_vida", columnDefinition = "TEXT")
    private String ondeApareceVida;

    /** Confusões conceptuais típicas (JSON: ["...", "..."]). */
    @Column(name = "erros_comuns", columnDefinition = "TEXT")
    private String errosComuns;

    /** Caso prático socrático (JSON: {"enunciado":"...","perguntas":["..."],"explicacao":"..."}). */
    @Column(name = "caso_pratico", columnDefinition = "TEXT")
    private String casoPratico;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "data_criacao", nullable = false)
    @Builder.Default
    private Instant dataCriacao = Instant.now();
}
