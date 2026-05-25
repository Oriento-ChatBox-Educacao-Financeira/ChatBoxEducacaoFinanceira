package com.oriento.api.model;

import com.oriento.api.model.enuns.OrigemContabil;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamento_contabil")
public class LancamentoContabil {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lancamento_seq")
    @SequenceGenerator(name = "lancamento_seq", sequenceName = "lancamento_id_lancamento_seq", allocationSize = 1)
    @Column(name = "id_lancamento")
    private Integer idlancamento;

    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa idempresa;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "descricao",nullable = false, length = 500)
    private String descricao;

    @Column(name = "documento", nullable = true)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "origem",
            nullable = false,
            columnDefinition = "VARCHAR(20) DEFAULT 'manual'"
    )
    private OrigemContabil origem = OrigemContabil.manual;

    @JoinColumn(name = "id_simulacao", nullable = true)
    private Simulacao idsimualcao;


    public LancamentoContabil(Integer idlancamento) {
        this.idlancamento = idlancamento;
    }

    public Integer getId_lancamento() {
        return idlancamento;
    }

    public void setId_lancamento(Integer idlancamento) {
        this.idlancamento = idlancamento;
    }

    public Empresa getId_empresa() {
        return idempresa;
    }

    public void setId_empresa(Empresa idempresa) {
        this.idempresa = idempresa;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public OrigemContabil getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemContabil origem) {
        this.origem = origem;
    }

    public Simulacao getIdsimualcao() {
        return idsimualcao;
    }

    public void setId_simualcao(Simulacao idsimualcao) {
        this.idsimualcao = idsimualcao;
    }
}
