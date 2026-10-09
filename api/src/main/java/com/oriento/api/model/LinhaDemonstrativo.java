package com.oriento.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Mapeamento da tabela {@code linha_demonstrativo} (relat\u00f3rios cont\u00e1beis).
 * Schema real:
 * <ul>
 *   <li>{@code id_linha} {@code bigserial} PK.</li>
 *   <li>{@code id_empresa} {@code int4} NOT NULL (FK l\u00f3gica para empresa).</li>
 *   <li>{@code tipo} {@code text} NOT NULL — check constraint do banco
 *       restringe a {@code DRE}, {@code BP}, {@code FLUXO_CAIXA}, {@code OUTRO}.</li>
 *   <li>{@code codigo_conta} {@code text} nullable (ex.: 3.01.01).</li>
 *   <li>{@code descricao} {@code text} NOT NULL.</li>
 *   <li>{@code periodo} {@code date} NOT NULL (sempre 1o do m\u00eas).</li>
 *   <li>{@code granularidade} {@code text} default {@code 'MENSAL'}.</li>
 *   <li>{@code valor} {@code numeric} NOT NULL.</li>
 *   <li>{@code arquivo_origem} {@code text} nullable.</li>
 *   <li>{@code data_upload} {@code timestamptz} default {@code now()}.</li>
 *   <li>{@code hash_arquivo} {@code text} nullable.</li>
 * </ul>
 */
@Entity
@Table(name = "linha_demonstrativo")
public class LinhaDemonstrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_linha")
    private Long idLinha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "tipo", nullable = false, columnDefinition = "text")
    private String tipo;

    @Column(name = "codigo_conta", columnDefinition = "text")
    private String codigoConta;

    @Column(name = "descricao", nullable = false, columnDefinition = "text")
    private String descricao;

    @Column(name = "periodo", nullable = false)
    private LocalDate periodo;

    @Column(name = "granularidade", nullable = false, columnDefinition = "text")
    private String granularidade = "MENSAL";

    @Column(name = "valor", nullable = false, precision = 19, scale = 4)
    private BigDecimal valor;

    @Column(name = "arquivo_origem", columnDefinition = "text")
    private String arquivoOrigem;

    @Column(name = "data_upload", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private OffsetDateTime dataUpload;

    @Column(name = "hash_arquivo", columnDefinition = "text")
    private String hashArquivo;

    public LinhaDemonstrativo() {
    }

    public Long getIdLinha() {
        return idLinha;
    }

    public void setIdLinha(Long idLinha) {
        this.idLinha = idLinha;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCodigoConta() {
        return codigoConta;
    }

    public void setCodigoConta(String codigoConta) {
        this.codigoConta = codigoConta;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getPeriodo() {
        return periodo;
    }

    public void setPeriodo(LocalDate periodo) {
        this.periodo = periodo;
    }

    public String getGranularidade() {
        return granularidade;
    }

    public void setGranularidade(String granularidade) {
        this.granularidade = granularidade;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getArquivoOrigem() {
        return arquivoOrigem;
    }

    public void setArquivoOrigem(String arquivoOrigem) {
        this.arquivoOrigem = arquivoOrigem;
    }

    public OffsetDateTime getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(OffsetDateTime dataUpload) {
        this.dataUpload = dataUpload;
    }

    public String getHashArquivo() {
        return hashArquivo;
    }

    public void setHashArquivo(String hashArquivo) {
        this.hashArquivo = hashArquivo;
    }
}
