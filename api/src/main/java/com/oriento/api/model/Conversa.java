package com.oriento.api.model;

import com.oriento.api.model.enuns.StatusConversa;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversa")
public class Conversa {

    @Id
    @Column(name = "id_conversa")
    private UUID idConversa;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "VARCHAR(20) DEFAULT 'ativa'"
    )
    private StatusConversa status = StatusConversa.ativa;

    @Column(
            name = "iniciada_em",
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT NOW()"
    )
    private LocalDateTime iniciadaEm = LocalDateTime.now();


    public UUID getIdConversa() {
        return idConversa;
    }

    public void setIdConversa(UUID idConversa) {
        this.idConversa = idConversa;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
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

    public LocalDateTime getIniciadaEm() {
        return iniciadaEm;
    }

    public void setIniciadaEm(LocalDateTime iniciadaEm) {
        this.iniciadaEm = iniciadaEm;
    }
}