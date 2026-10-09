package com.oriento.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapeamento da tabela {@code notificacao}. Schema:
 * <ul>
 *   <li>{@code id_notificacao} bigserial PK.</li>
 *   <li>{@code id_usuario} uuid NOT NULL.</li>
 *   <li>{@code tipo, titulo, mensagem} text NOT NULL.</li>
 *   <li>{@code link, icone} text nullable.</li>
 *   <li>{@code lida} bool default false.</li>
 *   <li>{@code data_criacao} timestamptz default now().</li>
 *   <li>{@code data_leitura} timestamptz nullable.</li>
 * </ul>
 */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Long idNotificacao;

    @Column(name = "id_usuario", nullable = false, columnDefinition = "uuid")
    private UUID idUsuario;

    @Column(name = "tipo", nullable = false, columnDefinition = "text")
    private String tipo;

    @Column(name = "titulo", nullable = false, columnDefinition = "text")
    private String titulo;

    @Column(name = "mensagem", nullable = false, columnDefinition = "text")
    private String mensagem;

    @Column(name = "link", columnDefinition = "text")
    private String link;

    @Column(name = "icone", columnDefinition = "text")
    private String icone;

    @Column(name = "lida", nullable = false)
    private boolean lida;

    @Column(name = "data_criacao", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private OffsetDateTime dataCriacao;

    @Column(name = "data_leitura")
    private OffsetDateTime dataLeitura;

    public Notificacao() {
    }

    public Long getIdNotificacao() {
        return idNotificacao;
    }

    public void setIdNotificacao(Long idNotificacao) {
        this.idNotificacao = idNotificacao;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(OffsetDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public OffsetDateTime getDataLeitura() {
        return dataLeitura;
    }

    public void setDataLeitura(OffsetDateTime dataLeitura) {
        this.dataLeitura = dataLeitura;
    }
}
