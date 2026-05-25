package com.oriento.api.dto;

import com.oriento.api.model.Usuario;

import java.util.UUID;

/**
 * Representa os dados públicos do usuário retornados para o frontend.
 */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email
) {

    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getEmail()
        );
    }
}

