package com.oriento.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conteudo_educacional")
public class ConteudoEducacional {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conteudo_educacional_seq")
    @SequenceGenerator(
            name = "conteudo_educacional_seq",
            sequenceName = "conteudo_educacional_id_conteudo_seq",
            allocationSize = 1
    )
    @Column(name = "id_conteudo")
    private Integer idConteudo;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "url_link", length = 255)
    private String urlLink;

    @Column(name = "categoria", length = 50)
    private String categoria;


    public Integer getIdConteudo() {
        return idConteudo;
    }

    public void setIdConteudo(Integer idConteudo) {
        this.idConteudo = idConteudo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getUrlLink() {
        return urlLink;
    }

    public void setUrlLink(String urlLink) {
        this.urlLink = urlLink;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}