package com.oriento.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.oriento.api.model.RefreshToken;
import com.oriento.api.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    /**
     * Retorna todos os refresh tokens de um usu\u00e1rio. Em um cen\u00e1rio
     * ideal s\u00f3 deveria existir um, mas em situa\u00e7\u00f5es de corrida
     * ou bugs hist\u00f3ricos m\u00faltiplos registros podem coexistir; a forma
     * de {@link List} evita o {@code NonUniqueResultException} que o tipo
     * {@link Optional} provocava.
     */
    List<RefreshToken> findAllByUsuario(Usuario usuario);

    @Modifying
    @Query("delete from RefreshToken r where r.usuario = :usuario")
    int deleteAllByUsuario(Usuario usuario);
}
