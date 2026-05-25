package com.oriento.api.model;

import com.oriento.api.model.enuns.TipoDemonstrativo;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "demonstrativo_oficial")
public class DemonstrativoOficial {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "demonstrativo_seq")
    @SequenceGenerator(name = "demonstrativo_seq", sequenceName = "demonstrativo_id_demonstrativo_seq", allocationSize = 1)
    @Column(name = "id_demonstrativo")
    private Integer idDemonstrativo;

    // Relacionamento com a Empresa (Muitos demonstrativos para Uma empresa)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDemonstrativo tipo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_json", nullable = false, columnDefinition = "JSONB")
    private String dadosJson;

    public DemonstrativoOficial(Integer idDemonstrativo) {
        this.idDemonstrativo = idDemonstrativo;
    }

    public Integer getIdDemonstrativo() {
        return idDemonstrativo;
    }

    public void setIdDemonstrativo(Integer idDemonstrativo) {
        this.idDemonstrativo = idDemonstrativo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public TipoDemonstrativo getTipo() {
        return tipo;
    }

    public void setTipo(TipoDemonstrativo tipo) {
        this.tipo = tipo;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public String getDadosJson() {
        return dadosJson;
    }

    public void setDadosJson(String dadosJson) {
        this.dadosJson = dadosJson;
    }
}
