package com.oriento.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "mensagens")
public class Mensagens {

    @Id
    private String id; // O Spring converte automaticamente para o ObjectId do Mongo

    @Field("conversa_id")
    private UUID conversaId;

    private Integer ordem;
    private String remetente; // "usuario" ou "ia"

    @Field("tipo_mensagem")
    private String tipoMensagem;

    private Conteudo conteudo;

    @Field("data_hora")
    private LocalDateTime dataHora = LocalDateTime.now();

    @Field("ia_metadados")
    private IaMetadados iaMetadados;

    @Field("feedback_usuario")
    private FeedbackUsuario feedbackUsuario;

    // --- CLASSES INTERNAS PARA ESTRUTURAR O JSON ---

    public static class Conteudo {
        public String texto;
        public Object dados_estruturados;
    }

    public static class IaMetadados {
        public String modelo_llm;
        public String intencao_detectada;
        public Object entidades_extraidas;
    }

    public Mensagens() {
    }

    public Mensagens(String id) {
        this.id = id;
    }

    public static class FeedbackUsuario {
        public Boolean util;
        public String comentario;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getConversaId() {
        return conversaId;
    }

    public void setConversaId(UUID conversaId) {
        this.conversaId = conversaId;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public String getRemetente() {
        return remetente;
    }

    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public String getTipoMensagem() {
        return tipoMensagem;
    }

    public void setTipoMensagem(String tipoMensagem) {
        this.tipoMensagem = tipoMensagem;
    }

    public Conteudo getConteudo() {
        return conteudo;
    }

    public void setConteudo(Conteudo conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public IaMetadados getIaMetadados() {
        return iaMetadados;
    }

    public void setIaMetadados(IaMetadados iaMetadados) {
        this.iaMetadados = iaMetadados;
    }

    public FeedbackUsuario getFeedbackUsuario() {
        return feedbackUsuario;
    }

    public void setFeedbackUsuario(FeedbackUsuario feedbackUsuario) {
        this.feedbackUsuario = feedbackUsuario;
    }
}