package com.oriento.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "simulacao")
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "simulacao_seq")
    @SequenceGenerator(
            name = "simulacao_seq",
            sequenceName = "simulacao_id_simulacao_seq",
            allocationSize = 1
    )
    @Column(name = "id_simulacao")
    private Integer idSimulacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "nome_cenario", nullable = false, length = 150)
    private String nomeCenario;

    @Column(name = "periodo_referencia", nullable = false)
    private LocalDate periodoReferencia;


    public Integer getIdSimulacao() {
        return idSimulacao;
    }

    public void setIdSimulacao(Integer idSimulacao) {
        this.idSimulacao = idSimulacao;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNomeCenario() {
        return nomeCenario;
    }

    public void setNomeCenario(String nomeCenario) {
        this.nomeCenario = nomeCenario;
    }

    public LocalDate getPeriodoReferencia() {
        return periodoReferencia;
    }

    public void setPeriodoReferencia(LocalDate periodoReferencia) {
        this.periodoReferencia = periodoReferencia;
    }
}