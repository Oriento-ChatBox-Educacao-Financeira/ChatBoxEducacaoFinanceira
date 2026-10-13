package com.oriento.api.model;

import com.oriento.api.model.enuns.Porte;
import com.oriento.api.model.enuns.Regimetributario;
import com.oriento.api.model.enuns.RegimetributarioConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

/**
 * Mapeamento da tabela {@code empresa}. Schema real do Postgres:
 * <ul>
 *   <li>{@code id_empresa} {@code int4} (sequence {@code empresa_id_empresa_seq}).</li>
 *   <li>{@code id_usuario} {@code uuid} nullable.</li>
 *   <li>{@code nome_fantasia} {@code varchar(150)} NOT NULL.</li>
 *   <li>{@code cnpj} {@code varchar(18)} NOT NULL UNIQUE (preserva zeros).</li>
 *   <li>{@code setor} {@code varchar(100)} nullable.</li>
 *   <li>{@code porte} tipo enum {@code porte_empresa} nullable.</li>
 *   <li>{@code regime_tributario} tipo enum {@code regime_tributario_tipo}
 *       NOT NULL (labels com espa\u00e7o, mapeados via converter).</li>
 *   <li>{@code data_criacao} {@code timestamptz} default {@code now()}.</li>
 *   <li>{@code razao_social} {@code text} nullable.</li>
 * </ul>
 *
 * <p>{@link DynamicInsert} omite do {@code INSERT} colunas {@code null} — assim
 * o {@code porte} (nullable) n\u00e3o aparece como {@code varchar} no bind quando
 * o usu\u00e1rio ainda n\u00e3o o preencheu.
 */
@Entity
@Table(name = "empresa")
@DynamicInsert
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "nome_fantasia", nullable = false, length = 150)
    private String nomeFantasia;

    @Column(name = "cnpj", unique = true, nullable = false, length = 18)
    private String cnpj;

    @Column(name = "razao_social", columnDefinition = "text")
    private String razaoSocial;

    @Column(name = "setor", length = 100)
    private String setor;

    @Enumerated(EnumType.STRING)
    @Column(name = "porte", columnDefinition = "porte_empresa")
    private Porte porte;

    @Convert(converter = RegimetributarioConverter.class)
    @Column(name = "regime_tributario", nullable = false, columnDefinition = "regime_tributario_tipo")
    private Regimetributario regimeTributario;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private LocalDateTime dataCriacao;

    public Empresa() {
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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
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

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
