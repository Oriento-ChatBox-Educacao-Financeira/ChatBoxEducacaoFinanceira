package com.oriento.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_lancamento")
public class ItemLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_seq")
    @SequenceGenerator(name = "item_seq", sequenceName = "item_id_item_seq", allocationSize = 1)
    @Column(name = "id_item")
    private Integer id;

    @ManyToOne
    @JoinColumn(
            name = "id_lancamento",
            nullable = false
    )
    private LancamentoContabil id_lancamento;

    @ManyToOne
    @JoinColumn(name = "id_conta",nullable = false)
    private PlanoConta id_conta;

    @ManyToOne
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LancamentoContabil getId_lancamento() {
        return id_lancamento;
    }

    public void setId_lancamento(LancamentoContabil id_lancamento) {
        this.id_lancamento = id_lancamento;
    }

    public PlanoConta getId_conta() {
        return id_conta;
    }

    public void setId_conta(PlanoConta id_conta) {
        this.id_conta = id_conta;
    }

    public CentroCusto getId_centro_custo() {
        return id_centro_custo;
    }

    public void setId_centro_custo(CentroCusto id_centro_custo) {
        this.id_centro_custo = id_centro_custo;
    }

    public BigDecimal getValorDebito() {
        return valorDebito;
    }

    public void setValorDebito(BigDecimal valorDebito) {
        this.valorDebito = valorDebito;
    }

    public BigDecimal getValorCredito() {
        return valorCredito;
    }

    public void setValorCredito(BigDecimal valorCredito) {
        this.valorCredito = valorCredito;
    }

}
