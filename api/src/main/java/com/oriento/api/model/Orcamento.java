package com.oriento.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamento")
public class Orcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orcamento_seq")
    @SequenceGenerator(name = "orcamento_seq", sequenceName = "orcamento_id_orcamento_seq", allocationSize = 1)
    @Column(name = "id_orcamento")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_empresa",nullable = false)
    private Empresa empresaId;

    @Column(name = "ano",nullable = false)
    private Integer ano;

    @Column(name = "mes",nullable = false)
    private Integer mes;

    @ManyToOne
    @JoinColumn(name = "id_conta", nullable = false)
    private PlanoConta contaId;

    @ManyToOne
    @JoinColumn(name = "id_centro_custo", nullable = false)
    private CentroCusto centroCustoId;

    @Column(name = "valor_planejado",nullable = false)
    private BigDecimal valorPlanejado;

    @Column(name = "aprovado",columnDefinition = "DEFAULT FALSE")
    private Boolean aprovado = false;


    public Orcamento(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Empresa getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Empresa empresaId) {
        this.empresaId = empresaId;
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

    public PlanoConta getContaId() {
        return contaId;
    }

    public void setContaId(PlanoConta contaId) {
        this.contaId = contaId;
    }

    public CentroCusto getCentroCustoId() {
        return centroCustoId;
    }

    public void setCentroCustoId(CentroCusto centroCustoId) {
        this.centroCustoId = centroCustoId;
    }

    public BigDecimal getValorPlanejado() {
        return valorPlanejado;
    }

    public void setValorPlanejado(BigDecimal valorPlanejado) {
        this.valorPlanejado = valorPlanejado;
    }

    public Boolean getAprovado() {
        return aprovado;
    }

    public void setAprovado(Boolean aprovado) {
        this.aprovado = aprovado;
    }
}
