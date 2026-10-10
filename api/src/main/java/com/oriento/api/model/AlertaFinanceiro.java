package com.oriento.api.model;

import com.oriento.api.model.enuns.SeveridadeFinanceiro;
import jakarta.persistence.*;

@Entity
@Table(name = "alerta_financeiro")
public class AlertaFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alerta_financeiro_seq")
    @SequenceGenerator(
            name = "alerta_financeiro_seq",
            sequenceName = "alerta_financeiro_id_alerta_seq",
            allocationSize = 1
    )
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "tipo_alerta", nullable = false, length = 50)
    private String tipoAlerta;

    @Column(name = "mensagem", nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "severidade",
            nullable = false,
            columnDefinition = "VARCHAR(20) DEFAULT 'media'"
    )
    private SeveridadeFinanceiro severidade = SeveridadeFinanceiro.media;

    public Integer getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(Integer idAlerta) {
        this.idAlerta = idAlerta;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(String tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public SeveridadeFinanceiro getSeveridade() {
        return severidade;
    }

    public void setSeveridade(SeveridadeFinanceiro severidade) {
        this.severidade = severidade;
    }
}