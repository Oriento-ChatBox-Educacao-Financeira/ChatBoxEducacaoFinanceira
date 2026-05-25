package com.oriento.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "indicador_financeiro",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_empresa_periodo_indicador",
                        columnNames = {"id_empresa", "ano_mes"}
                )
        }
)
public class IndicadorFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "indicador_seq")
    @SequenceGenerator(name = "indicador_seq", sequenceName = "indicador_id_indicador_seq", allocationSize = 1)
    @Column(name = "id_indicador")
    private Integer idIndicador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "ano_mes", nullable = false)
    private LocalDate anoMes;

    @Column(name = "receita_total", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal receitaTotal = BigDecimal.ZERO;

    @Column(name = "despesas_totais", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal despesasTotais = BigDecimal.ZERO;

    @Column(name = "lucro_liquido", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal lucroLiquido = BigDecimal.ZERO;


    @Column(name = "margem_lucro", columnDefinition = "DECIMAL(5,2)")
    private BigDecimal margemLucro;

    @Column(name = "fluxo_caixa", columnDefinition = "DECIMAL(15,2)")
    private BigDecimal fluxoCaixa;

    @Column(name = "explicacao_automatica", columnDefinition = "TEXT")
    private String explicacaoAutomatica;


    public Integer getIdIndicador() {
        return idIndicador;
    }

    public void setIdIndicador(Integer idIndicador) {
        this.idIndicador = idIndicador;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public LocalDate getAnoMes() {
        return anoMes;
    }

    public void setAnoMes(LocalDate anoMes) {
        this.anoMes = anoMes;
    }

    public BigDecimal getReceitaTotal() {
        return receitaTotal;
    }

    public void setReceitaTotal(BigDecimal receitaTotal) {
        this.receitaTotal = receitaTotal;
    }

    public BigDecimal getDespesasTotais() {
        return despesasTotais;
    }

    public void setDespesasTotais(BigDecimal despesasTotais) {
        this.despesasTotais = despesasTotais;
    }

    public BigDecimal getLucroLiquido() {
        return lucroLiquido;
    }

    public void setLucroLiquido(BigDecimal lucroLiquido) {
        this.lucroLiquido = lucroLiquido;
    }

    public BigDecimal getMargemLucro() {
        return margemLucro;
    }

    public void setMargemLucro(BigDecimal margemLucro) {
        this.margemLucro = margemLucro;
    }

    public BigDecimal getFluxoCaixa() {
        return fluxoCaixa;
    }

    public void setFluxoCaixa(BigDecimal fluxoCaixa) {
        this.fluxoCaixa = fluxoCaixa;
    }

    public String getExplicacaoAutomatica() {
        return explicacaoAutomatica;
    }

    public void setExplicacaoAutomatica(String explicacaoAutomatica) {
        this.explicacaoAutomatica = explicacaoAutomatica;
    }
}