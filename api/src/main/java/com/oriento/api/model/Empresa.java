package com.oriento.api.model;

import com.oriento.api.model.enuns.Porte;
import com.oriento.api.model.enuns.Regimetributario;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id_empresa")
    private Integer id;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "nome_fantasia",nullable = false,length = 150)
    private String nomeFantasia;

    @Column(name = "cnpj",unique = true,nullable = false,length = 18)
    private Long cnpj; //ADICIONAR FORMATAÇÃO DO CNPJ

    @Column(name = "setor",length = 100,nullable = false)
    private String setor;

    @Enumerated(EnumType.STRING)
    @Column(name = "porte",nullable = true)
    private Porte porte;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tributario",nullable = true)
    private Regimetributario regimeTributario;

    @CreationTimestamp
    @Column(name = "data_criacao",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT NOW()")
    private LocalDateTime datacriacao;

    public Empresa(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public Long getCnpj() {
        return cnpj;
    }

    public void setCnpj(Long cnpj) {
        this.cnpj = cnpj;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }

    public Porte getPorte() {
        return porte;
    }

    public void setPorte(Porte porte) {
        this.porte = porte;
    }

    public Regimetributario getRegimeTributario() {
        return regimeTributario;
    }

    public void setRegimeTributario(Regimetributario regimeTributario) {
        this.regimeTributario = regimeTributario;
    }

    public LocalDateTime getData_criacao() {
        return datacriacao;
    }

    public void setData_criacao(LocalDateTime data_criacao) {
        this.datacriacao = data_criacao;
    }
}
