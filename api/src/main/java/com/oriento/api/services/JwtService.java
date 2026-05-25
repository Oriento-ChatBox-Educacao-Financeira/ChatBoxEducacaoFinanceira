package com.oriento.api.services;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oriento.api.model.Usuario;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private RSAPrivateKey privateKey;

    private static final long ACCESS_TOKEN_DURATION = 900L; // 15 * 60 segundos

    public String gerarTokenJWT(Usuario usuario) {
        logger.debug("Gerando token JWT para usuário ID: {}", usuario.getIdUsuario());
        
        var now = Instant.now();
        var expiration = now.plusSeconds(ACCESS_TOKEN_DURATION);

        var token = Jwts.builder()
                .issuer("oriento")
                .subject(usuario.getIdUsuario().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey)
                .compact();

        logger.debug("Token JWT gerado com sucesso para usuário ID: {}. Expira em: {}", 
                usuario.getIdUsuario(), expiration);
        
        return token;
    }

    public Long getAccessTokenDuration() {
        return ACCESS_TOKEN_DURATION;
    }

}
