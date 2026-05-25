package com.oriento.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oriento.api.model.RefreshToken;
import com.oriento.api.model.Usuario;

import java.util.Optional;

public interface    RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUsuario(Usuario usuario);
}
