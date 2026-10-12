package com.oriento.api.model;


import com.oriento.api.model.enuns.TipoSimulacao;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parametro_simulacao")
public class ParametroSimulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parametro_simulacao_seq")
    @SequenceGenerator(
            name = "parametro_simulacao_seq",
            sequenceName = "parametro_simulacao_id_parametro_seq",
            allocationSize = 1
    )
    @Column(name = "id_parametro")
    private Integer idParametro;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_simulacao", nullable = false)
    private Simulacao simulacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoSimulacao tipo;


    @Column(name = "variacao_percentual", columnDefinition = "DECIMAL(6,2)")
    private BigDecimal variacaoPercentual;


    @Column(name = "valor_absoluto", columnDefinition = "DECIMAL(15,2)")
    private BigDecimal valorAbsoluto;

    public Integer getIdParametro() {
        return idParametro;
    }

    public void setIdParametro(Integer idParametro) {
        this.idParametro = idParametro;
    }

    public Simulacao getSimulacao() {
        return simulacao;
    }

    public void setSimulacao(Simulacao simulacao) {
        this.simulacao = simulacao;
    }

    public TipoSimulacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoSimulacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getVariacaoPercentual() {
        return variacaoPercentual;
    }

    public void setVariacaoPercentual(BigDecimal variacaoPercentual) {
        this.variacaoPercentual = variacaoPercentual;
    }

    public BigDecimal getValorAbsoluto() {
        return valorAbsoluto;
    }

    public void setValorAbsoluto(BigDecimal valorAbsoluto) {
        this.valorAbsoluto = valorAbsoluto;
    }
}