package com.oriento.api.model;

import com.oriento.api.model.enuns.StatusConversa;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversa")
public class AIConversation {

    @Id
    @Column(name = "id_conversa", columnDefinition = "UUID")
    private UUID idConversa;   // ou String, mas UUID é mais adequado

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_empresa")
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusConversa status;   // enum com os valores possíveis

    @Column(name = "iniciada_em")
    private OffsetDateTime iniciadaEm;

    public UUID getIdConversa() {
        return idConversa;
    }

    public void setIdConversa(UUID idConversa) {
        this.idConversa = idConversa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public StatusConversa getStatus() {
        return status;
    }

    public void setStatus(StatusConversa status) {
        this.status = status;
    }

    public OffsetDateTime getIniciadaEm() {
        return iniciadaEm;
    }

    public void setIniciadaEm(OffsetDateTime iniciadaEm) {
        this.iniciadaEm = iniciadaEm;
    }

    public AIConversation(UUID idConversa, Usuario usuario) {
        this.idConversa = idConversa;
        this.usuario = usuario;
    }

    public AIConversation() {
    }

    public boolean pertenceAo(Usuario usuario) {
        return this.usuario != null
                && usuario != null
                && this.usuario.getIdUsuario() != null
                && this.usuario.getIdUsuario().equals(usuario.getIdUsuario());
    }
}

