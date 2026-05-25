package com.oriento.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_lancamento")
public class ItemLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_seq")
    @SequenceGenerator(name = "item_seq", sequenceName = "item_id_item_seq", allocationSize = 1)
    @Column(name = "id_lancamento")
    private Integer id;

    @JoinColumn(
            name = "id_lancamento",
            nullable = false
    )
    private LancamentoContabil id_lancamento;

    @JoinColumn(name = "id_conta",nullable = false)
    private PlanoConta id_conta;

    @JoinColumn(name = "id_centro_custo",nullable = true)
    private CentroCusto id_centro_custo;

    @Column(
            name = "valor_debito",
            nullable = false,
            columnDefinition = "DECIMAL(15,2) DEFAULT 0.00"
    )
    private BigDecimal valorDebito = BigDecimal.ZERO;

    @Column(
            name = "valor_credito",
            nullable = false,
            columnDefinition = "DECIMAL(15,2) DEFAULT 0.00"
    )
    private BigDecimal valorCredito = BigDecimal.ZERO;

}
