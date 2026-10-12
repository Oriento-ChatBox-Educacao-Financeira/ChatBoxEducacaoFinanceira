package com.oriento.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "saldo_conta",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_empresa_conta_periodo",
                        columnNames = {"id_empresa", "id_conta", "ano", "mes"}
                )
        }
)
public class SaldoConta {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saldo_seq")
    @SequenceGenerator(name = "saldo_seq", sequenceName = "saldo_id_saldo_seq", allocationSize = 1)
    @Column(name = "id_saldo")
    private Integer idSaldo;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conta", nullable = false)
    private PlanoConta conta;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @Column(name = "saldo_inicial", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "debito_periodo", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal debitoPeriodo = BigDecimal.ZERO;

    @Column(name = "credito_periodo", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal creditoPeriodo = BigDecimal.ZERO;

    @Column(name = "saldo_final", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal saldoFinal = BigDecimal.ZERO;


    public SaldoConta(Integer idSaldo) {
        this.idSaldo = idSaldo;
    }

    public Integer getIdSaldo() {
        return idSaldo;
    }

    public void setIdSaldo(Integer idSaldo) {
        this.idSaldo = idSaldo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public PlanoConta getConta() {
        return conta;
    }

    public void setConta(PlanoConta conta) {
        this.conta = conta;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getDebitoPeriodo() {
        return debitoPeriodo;
    }

    public void setDebitoPeriodo(BigDecimal debitoPeriodo) {
        this.debitoPeriodo = debitoPeriodo;
    }

    public BigDecimal getCreditoPeriodo() {
        return creditoPeriodo;
    }

    public void setCreditoPeriodo(BigDecimal creditoPeriodo) {
        this.creditoPeriodo = creditoPeriodo;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }
}
