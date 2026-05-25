package com.oriento.api.model;

import com.oriento.api.dto.LoginRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id_usuario")
    private UUID idUsuario;

    @Column(unique = true)
    private String cnpj;

    @Column(unique = true)
    private String email;

    private String nome;

    private String razaoSocial;

    private String nomeFantasia;

    private String senha;

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
    public String getCnpj() {
        return cnpj;
    }
    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
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
    public String getRazaoSocial() {
        return razaoSocial;
    }
    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }
    public String getNomeFantasia() {
        return nomeFantasia;
    }
    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
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

}
