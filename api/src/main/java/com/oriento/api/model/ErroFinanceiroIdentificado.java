package com.oriento.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erro_financeiro_identificado")
public class ErroFinanceiroIdentificado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "erro_financeiro_seq")
    @SequenceGenerator(
            name = "erro_financeiro_seq",
            sequenceName = "erro_financeiro_id_erro_seq",
            allocationSize = 1
    )
    @Column(name = "id_erro")
    private Integer idErro;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;


    @Column(name = "descricao_erro", nullable = false, columnDefinition = "TEXT")
    private String descricaoErro;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conteudo_recomendo", nullable = true) // Aceita NULL se não houver recomendação específica
    private ConteudoEducacional conteudoRecomendado;


    public Integer getIdErro() {
        return idErro;
    }

    public void setIdErro(Integer idErro) {
        this.idErro = idErro;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getDescricaoErro() {
        return descricaoErro;
    }

    public void setDescricaoErro(String descricaoErro) {
        this.descricaoErro = descricaoErro;
    }

    public ConteudoEducacional getConteudoRecomendado() {
        return conteudoRecomendado;
    }

    public void setConteudoRecomendado(ConteudoEducacional conteudoRecomendado) {
        this.conteudoRecomendado = conteudoRecomendado;
    }
}