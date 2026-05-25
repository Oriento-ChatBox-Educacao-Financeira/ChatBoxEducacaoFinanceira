package com.oriento.api.model;

import com.oriento.api.model.enuns.TipoCentroCusto;
import jakarta.persistence.*;
import jakarta.validation.groups.Default;

@Entity
@Table(name = "centro_custo")
public class CentroCusto{
    @Id
    @Column(name = "id_centro_custo")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer idcentrocusto;

    @JoinColumn(name = "id_empresa", nullable = false)
    @ManyToOne
    private Empresa id_empresa;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCentroCusto tipo;

    @Column(name = "ativo",
            nullable = false,
            columnDefinition ="BOOLEAN DEFAULT TRUE"
    )
    private Boolean ativo = true;
}
