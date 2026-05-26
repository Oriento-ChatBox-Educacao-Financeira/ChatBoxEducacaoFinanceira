package com.oriento.api.services;

import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Resolve o {@link Usuario} corrente a partir do JWT em
 * {@link SecurityContextHolder}. Centraliza a valida\u00e7\u00e3o do {@code sub}
 * (UUID) e o lookup no reposit\u00f3rio, evitando que cada controller
 * reimplemente esse fluxo.
 */
@Service
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obterUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usu\u00e1rio n\u00e3o autenticado");
        }
        return obterUsuarioPorJwt(jwt);
    }

    public Usuario obterUsuarioPorJwt(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usu\u00e1rio n\u00e3o autenticado");
        }
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inv\u00e1lido");
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usu\u00e1rio n\u00e3o encontrado"));
    }
}
