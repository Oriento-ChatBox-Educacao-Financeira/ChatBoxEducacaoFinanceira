package com.oriento.api.model;

import com.oriento.api.model.enuns.TipoCentroCusto;
import jakarta.persistence.*;
import jakarta.validation.groups.Default;

@Entity
@Table(name = "centro_custo")
public class CentroCusto{
    @Id
    @Column(name = "id_centro_custo")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer idcentrocusto;

    @JoinColumn(name = "id_empresa", nullable = false)
    @ManyToOne
    private Empresa id_empresa;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCentroCusto tipo;

    @Column(name = "ativo",
            nullable = false,
            columnDefinition ="BOOLEAN DEFAULT TRUE"
    )
    private Boolean ativo = true;

    public Integer getIdcentrocusto() {
        return idcentrocusto;
    }

    public void setIdcentrocusto(Integer idcentrocusto) {
        this.idcentrocusto = idcentrocusto;
    }

    public Empresa getId_empresa() {
        return id_empresa;
    }

    public void setId_empresa(Empresa id_empresa) {
        this.id_empresa = id_empresa;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoCentroCusto getTipo() {
        return tipo;
    }

    public void setTipo(TipoCentroCusto tipo) {
        this.tipo = tipo;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
