package com.oriento.api.model;

import com.oriento.api.model.enuns.NaturezaPlanoConta;
import com.oriento.api.model.enuns.TipoPlanoConta;
import jakarta.persistence.*;
import jakarta.validation.groups.Default;
import org.aspectj.weaver.loadtime.definition.Definition;

@Entity
@Table(name = "plano_conta")
public class PlanoConta {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conta_seq")
    @SequenceGenerator(name = "conta_seq", sequenceName = "conta_id_conta_seq", allocationSize = 1)
    @Column(name = "id_conta")
    private Integer idConta;

    @JoinColumn(name = "id_empresa",nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Empresa idEmpresa;

    @Column(name = "codigo",length = 20,nullable = false)
    private String codigo;  //codigo hierarquico

    @Column(name = "nome", nullable = false,length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoPlanoConta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "natureza", nullable = false)
    private NaturezaPlanoConta natureza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conta_pai", nullable = true)
    private PlanoConta contaPai;

    @Column(name = "nivel",nullable = false)
    private Integer nivel;
    @Column(
            name = "permite_lancamento",
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT TRUE"
    )
    private Boolean permitelancamento = true;
}
