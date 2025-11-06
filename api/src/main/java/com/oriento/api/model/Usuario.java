package com.oriento.api.model;

import com.oriento.api.dto.LoginRequest;
import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

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

    /**
     * ID único do usuário (chave primária).
     * Gerado automaticamente como UUID pelo JPA.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id_usuario")
    private UUID idUsuario;
    
    /**
     * CNPJ do usuário (empresa).
     * Deve ser único no banco de dados (constraint UNIQUE).
     * Pode ser usado para login junto com a senha.
     */
    @Column(unique = true)
    private String cnpj;
    
    /**
     * Email do usuário.
     * Deve ser único no banco de dados (constraint UNIQUE).
     * Pode ser usado para login junto com a senha.
     */
    @Column(unique = true)
    private String email;
    
    /**
     * Nome do usuário (pessoa física).
     */
    private String nome;
    
    /**
     * Razão social da empresa.
     */
    private String razaoSocial;
    
    /**
     * Nome fantasia da empresa.
     */
    private String nomeFantasia;
    
    /**
     * Senha do usuário armazenada como hash BCrypt.
     * 
     * IMPORTANTE: A senha nunca deve ser armazenada em texto plano.
     * Sempre use BCryptPasswordEncoder para criptografar antes de salvar.
     */
    private String senha;

    /**
     * Verifica se as credenciais de login fornecidas correspondem a este usuário.
     * 
     * Este método compara a senha fornecida no LoginRequest com a senha
     * armazenada (hash BCrypt) usando o PasswordEncoder. O BCrypt faz a
     * comparação de forma segura, sem precisar descriptografar o hash.
     * 
     * @param loginRequest DTO contendo a senha fornecida pelo usuário
     * @param passwordEncoder Encoder BCrypt para comparar senhas
     * @return true se a senha fornecida corresponde à senha armazenada, false caso contrário
     */
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

}
