package com.oriento.api.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entidade JPA que representa um refresh token no banco de dados.
 * 
 * Refresh tokens são tokens de longa duração (15 dias) usados para renovar
 * access tokens sem necessidade de fazer login novamente. Eles são armazenados
 * no banco de dados para permitir validação e invalidação (logout).
 * 
 * Características:
 * - Cada usuário pode ter apenas um refresh token ativo por vez
 * - O token é um UUID único gerado aleatoriamente
 * - Possui data de expiração para controle de validade
 * - Relacionamento Many-to-One com Usuario (muitos tokens podem pertencer a um usuário,
 *   mas apenas um é ativo por vez devido à lógica de negócio)
 * 
 * Tabela no banco: refresh_token
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    
    /**
     * ID único do refresh token (chave primária).
     * Gerado automaticamente pelo banco de dados usando IDENTITY.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário proprietário deste refresh token.
     * 
     * Relacionamento Many-to-One: muitos refresh tokens podem pertencer a um usuário,
     * mas na prática apenas um é mantido ativo por vez (tokens antigos são removidos
     * quando um novo é criado).
     */
    @ManyToOne
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario")
    private Usuario usuario;

    /**
     * Token único (UUID) que identifica este refresh token.
     * 
     * Este é o valor que o cliente envia para renovar o access token.
     * Deve ser único no banco de dados (constraint UNIQUE).
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Data e hora de expiração do refresh token.
     * 
     * Após esta data, o token não pode mais ser usado para renovar access tokens.
     * O usuário precisará fazer login novamente.
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * @return ID único do refresh token
     */
    public Long getId() {
        return id;
    }

    /**
     * Define o ID único do refresh token.
     * 
     * @param id ID a ser definido
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return Usuário proprietário deste refresh token
     */
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * Define o usuário proprietário deste refresh token.
     * 
     * @param usuario Usuário a ser associado ao token
     */
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * @return Token único (UUID) que identifica este refresh token
     */
    public String getToken() {
        return token;
    }

    /**
     * Define o token único (UUID) deste refresh token.
     * 
     * @param token Token UUID a ser definido
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return Data e hora de expiração do refresh token
     */
    public Instant getExpiryDate() {
        return expiryDate;
    }

    /**
     * Define a data e hora de expiração do refresh token.
     * 
     * @param expiryDate Data/hora de expiração a ser definida
     */
    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }
}
