package com.oriento.api.model;

import com.oriento.api.dto.LoginRequest;
import com.oriento.api.model.enuns.Nivelmaturidadefinanceira;
import com.oriento.api.model.enuns.RoleUsuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;
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
@DynamicInsert
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_usuario", columnDefinition = "uuid")
    private UUID idUsuario;

    @Column(name = "email", length = 150, unique = true, nullable = false)
    private String email;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @Column(name = "senha_hash", length = 255, nullable = false)
    private String senha;

    /**
     * Persistido como {@code varchar(20)}. O DB tem default {@code 'BASICO'},
     * que est\u00e1 fora do enum {@code Nivelmaturidadefinanceira}; por isso
     * usamos {@link String} e adicionamos uma camada de UI/DTO se quisermos
     * restringir.
     */
    @Column(name = "nivel_maturidade", length = 20)
    private String nivelMaturidade;

    @Column(name = "data_criacao", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private OffsetDateTime dataCriacao;

    @Column(name = "ultimo_acesso", nullable = true)
    private LocalDateTime ultimoacesso;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private RoleUsuario role = RoleUsuario.ROLE_USER;

    @Column(name = "ultimo_acesso")
    private OffsetDateTime ultimoAcesso;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AIConversation> conversations = new HashSet<>();

    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = false)
    private Empresa empresa;

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

    public String getNivelMaturidade() {
        return nivelMaturidade;
    }

    public void setNivelMaturidade(String nivelMaturidade) {
        this.nivelMaturidade = nivelMaturidade;
    }

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(OffsetDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public OffsetDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(OffsetDateTime ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
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

    public Empresa getEmpresa() {
        return empresa;
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
    public RoleUsuario getRole() {
        return role;
    }
    public void setRole(RoleUsuario role) {
        this.role = role;
    }
}
