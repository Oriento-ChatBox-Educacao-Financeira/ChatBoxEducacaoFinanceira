package com.oriento.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "resultado_simulacao")
public class ResultadoSimulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resultado_simulacao_seq")
    @SequenceGenerator(
            name = "resultado_simulacao_seq",
            sequenceName = "resultado_simulacao_id_resultado_seq",
            allocationSize = 1
    )
    @Column(name = "id_resultado")
    private Integer idResultado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_simulacao", nullable = false)
    private Simulacao simulacao;

    @Column(name = "lucro_projetado", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal lucroProjetado;

    @Column(name = "margem_projetada", nullable = false, columnDefinition = "DECIMAL(5,2)")
    private BigDecimal margemProjetada;

    @Column(name = "fluxo_caixa_projetado", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal fluxoCaixaProjetado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grafico_dados", columnDefinition = "JSONB")
    private String graficoDados;


    public Integer getIdResultado() {
        return idResultado;
    }

    public void setIdResultado(Integer idResultado) {
        this.idResultado = idResultado;
    }

    public Simulacao getSimulacao() {
        return simulacao;
    }

    public void setSimulacao(Simulacao simulacao) {
        this.simulacao = simulacao;
    }

    public BigDecimal getLucroProjetado() {
        return lucroProjetado;
    }

    public void setLucroProjetado(BigDecimal lucroProjetado) {
        this.lucroProjetado = lucroProjetado;
    }

    public BigDecimal getMargemProjetada() {
        return margemProjetada;
    }

    public void setMargemProjetada(BigDecimal margemProjetada) {
        this.margemProjetada = margemProjetada;
    }

    public BigDecimal getFluxoCaixaProjetado() {
        return fluxoCaixaProjetado;
    }

    public void setFluxoCaixaProjetado(BigDecimal fluxoCaixaProjetado) {
        this.fluxoCaixaProjetado = fluxoCaixaProjetado;
    }

    public String getGraficoDados() {
        return graficoDados;
    }

    public void setGraficoDados(String graficoDados) {
        this.graficoDados = graficoDados;
    }
}