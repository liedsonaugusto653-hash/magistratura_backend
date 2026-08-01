package ao.magistratura.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "historico_estudo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoEstudo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id")
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diploma_id")
    private Diploma diploma;

    @Column(name = "tempo_segundos", nullable = false)
    private Integer tempoSegundos;

    @Column(name = "data", nullable = false)
    private Instant data;

    @Column(name = "ultima_pagina")
    private Integer ultimaPagina;
}
