package com.oriento.api.services;

import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticatedUserService {
    private final UsuarioRepository repository;

    public AuthenticatedUserService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario resolve(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
            if (!id.toString().equalsIgnoreCase(jwt.getSubject())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
    }
}
