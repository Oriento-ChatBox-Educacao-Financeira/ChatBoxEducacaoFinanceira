package com.oriento.api.services;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private RSAPrivateKey privateKey;

    private static final long ACCESS_TOKEN_DURATION = 900L; // 15 * 60 segundos

    /**
     * Gera o token JWT com as claims customizadas de contexto do usuário.
     *
     * O ID do usuário é armazenado no campo padrão RFC 7519 "sub" (subject),
     * não como claim customizada, evitando duplicação de dados no payload.
     *
     * Claims emitidas:
     * - sub       → UUID do usuário (padrão JWT)
     * - empresaId → ID da empresa vinculada (pode ser null)
     * - email     → e-mail do usuário
     * - roles     → lista de papéis de acesso (ex: ["ROLE_USER"])
     *
     * @param usuario o usuário autenticado
     * @param empresa a empresa associada ao usuário (pode ser null)
     * @return token JWT assinado com RSA
     */
    public String gerarTokenJWT(Usuario usuario, Empresa empresa) {
        logger.debug("Gerando token JWT para usuário ID: {}", usuario.getIdUsuario());

        var now        = Instant.now();
        var expiration = now.plusSeconds(ACCESS_TOKEN_DURATION);

        var token = Jwts.builder()
                .issuer("oriento")
                .subject(usuario.getIdUsuario().toString()) // ID no subject (padrão RFC 7519)
                .claim("empresaId", empresa != null ? empresa.getId() : null)
                .claim("email",     usuario.getEmail())
                .claim("roles",     List.of(usuario.getRole().name()))
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
