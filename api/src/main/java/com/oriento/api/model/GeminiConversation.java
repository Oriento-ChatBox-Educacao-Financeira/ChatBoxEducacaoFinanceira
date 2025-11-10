package com.oriento.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidade que representa uma conversa com o assistente Gemini.
 *
 * Cada conversa está associada a um único usuário, enquanto
 * um usuário pode possuir várias conversas registradas.
 */
@Entity
@Table(name = "gemini_conversation")
public class GeminiConversation {

    @Id
    @Column(name = "conversation_id", nullable = false, updatable = false, length = 60)
    private String conversationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Construtor padrão exigido pelo JPA.
     */
    protected GeminiConversation() {
        // Utilizado pelo JPA
    }

    public GeminiConversation(String conversationId, Usuario usuario) {
        this.conversationId = conversationId;
        this.usuario = usuario;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public String getConversationId() {
        return conversationId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean pertenceAo(Usuario usuario) {
        return this.usuario != null
                && usuario != null
                && this.usuario.getIdUsuario() != null
                && this.usuario.getIdUsuario().equals(usuario.getIdUsuario());
    }
}

