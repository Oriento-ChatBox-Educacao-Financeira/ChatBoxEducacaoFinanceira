package com.oriento.api.model;

import com.oriento.api.model.enuns.NaturezaPlanoConta;
import com.oriento.api.model.enuns.TipoPlanoConta;
import jakarta.persistence.*;
import jakarta.validation.groups.Default;
import org.aspectj.weaver.loadtime.definition.Definition;

@Entity
@Table(name = "plano_conta")
public class PlanoConta {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conta_seq")
    @SequenceGenerator(name = "conta_seq", sequenceName = "conta_id_conta_seq", allocationSize = 1)
    @Column(name = "id_conta")
    private Integer idConta;

    @JoinColumn(name = "id_empresa",nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Empresa idEmpresa;

    @Column(name = "codigo",length = 20,nullable = false)
    private String codigo;  //codigo hierarquico

    @Column(name = "nome", nullable = false,length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoPlanoConta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "natureza", nullable = false)
    private NaturezaPlanoConta natureza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conta_pai", nullable = true)
    private PlanoConta contaPai;

    @Column(name = "nivel",nullable = false)
    private Integer nivel;
    @Column(
            name = "permite_lancamento",
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT TRUE"
    )
    private Boolean permitelancamento = true;

    public Integer getIdConta() {
        return idConta;
    }

    public void setIdConta(Integer idConta) {
        this.idConta = idConta;
    }

    public Empresa getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Empresa idEmpresa) {
        this.idEmpresa = idEmpresa;
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

    public TipoPlanoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoPlanoConta tipo) {
        this.tipo = tipo;
    }

    public NaturezaPlanoConta getNatureza() {
        return natureza;
    }

    public void setNatureza(NaturezaPlanoConta natureza) {
        this.natureza = natureza;
    }

    public PlanoConta getContaPai() {
        return contaPai;
    }

    public void setContaPai(PlanoConta contaPai) {
        this.contaPai = contaPai;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public Boolean getPermitelancamento() {
        return permitelancamento;
    }

    public void setPermitelancamento(Boolean permitelancamento) {
        this.permitelancamento = permitelancamento;
    }
}
