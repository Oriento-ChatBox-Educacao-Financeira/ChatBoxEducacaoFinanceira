package com.oriento.api.model;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.oriento.api.dto.LoginRequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entidade JPA da tabela {@code usuario}.
 * <ul>
 *   <li>{@code id_usuario} uuid (default {@code gen_random_uuid()} no banco).</li>
 *   <li>{@code email} {@code varchar(150)} NOT NULL UNIQUE.</li>
 *   <li>{@code senha_hash} {@code varchar(255)} NOT NULL (BCrypt).</li>
 *   <li>{@code nivel_maturidade} {@code varchar(20)} nullable, default {@code 'BASICO'} no banco.</li>
 *   <li>{@code data_criacao} {@code timestamptz} default {@code now()}.</li>
 *   <li>{@code ultimo_acesso} {@code timestamptz} nullable.</li>
 * </ul>
 *
 * <p>{@link DynamicInsert} omite colunas {@code null} no insert para que o
 * {@code DEFAULT 'BASICO'} do banco seja respeitado quando o c\u00f3digo n\u00e3o
 * informar o {@code nivel_maturidade}.
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

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}
