package com.oriento.api.model;

import com.oriento.api.dto.LoginRequest;
import com.oriento.api.model.enuns.Nivelmaturidadefinanceira;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Entidade JPA que representa um usuário no banco de dados.
 * 
 * Esta entidade armazena todas as informações de um usuário do sistema,
 * incluindo dados pessoais e credenciais de acesso.
 * 
 * Características:
 * - ID único gerado automaticamente (UUID)
 * - Email e CNPJ únicos (constraints UNIQUE)
 * - Senha armazenada como hash BCrypt (nunca em texto plano)
 * - Suporta login por email ou CNPJ
 * 
 * Tabela no banco: usuario
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id_usuario", columnDefinition = "UUID")
    private UUID idUsuario;

    @Column(name = "email",
            length = 150,
            unique = true,
            nullable = false)
    private String email;

    @Column(name = "nome",
            length = 100,
            nullable = false)
    private String nome;

    @Column(name = "senha_hash",
            length = 255,
            nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "nivel_maturidade",
            nullable = false
    )
    private Nivelmaturidadefinanceira nivelMaturidadeUser;

    @CreationTimestamp
    @Column(name = "data_criacao",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT NOW()")
    private LocalDateTime datacriacao;

    @Column(name = "ultimo_acesso", nullable = true)
    private LocalDateTime ultimoacesso;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AIConversation> conversations = new HashSet<>();

    public boolean verificarLogin(LoginRequest loginRequest, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(loginRequest.senha(), this.senha);
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }
    public Set<AIConversation> getConversations() {
        return conversations;
    }
    public void setConversations(Set<AIConversation> conversations) {
        this.conversations = conversations;
    }
    public void addConversation(AIConversation conversation) {
        conversations.add(conversation);
        conversation.setUsuario(this);
    }
    public void removeConversation(AIConversation conversation) {
        conversations.remove(conversation);
        conversation.setUsuario(null);
    }
    public Nivelmaturidadefinanceira getNivelMaturidadeUser() {
        return nivelMaturidadeUser;
    }
    public void setNivelMaturidadeUser(Nivelmaturidadefinanceira nivelMaturidadeUser) {
        this.nivelMaturidadeUser = nivelMaturidadeUser;
    }
    public LocalDateTime getData_criacao() {
        return datacriacao;
    }
    public void setData_criacao(LocalDateTime data_criacao) {
        this.datacriacao = LocalDateTime.now();
    }
    public LocalDateTime getUltimo_acesso() {
        return ultimoacesso;
    }
    public void setUltimoacesso(LocalDateTime ultimo_acesso) {
        this.ultimoacesso = ultimo_acesso;
    }
}
