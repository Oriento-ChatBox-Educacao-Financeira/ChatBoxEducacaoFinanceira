package com.oriento.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Conversor customizado de JWT para o token de autenticação do Spring Security.
 *
 * Responsabilidades:
 * 1. Lê as claims customizadas do JWT (userId, empresaId, email, roles)
 * 2. Converte a claim "roles" em GrantedAuthority, que o Spring Security entende
 * 3. Monta o objeto UsuarioAutenticado como Principal
 * 4. Retorna um JwtAuthenticationToken com o Principal e as authorities
 *
 * Após registrado no AuthConfig, substitui o comportamento padrão do Spring,
 * que só leria a claim "scope" ou "scp".
 */
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(CustomJwtAuthenticationConverter.class);

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        // 1. Extrai as authorities (roles) da claim "roles" do token
        Collection<GrantedAuthority> authorities = extrairAuthorities(jwt);

        // 2. Monta o Principal customizado com os dados de contexto do usuário
        UsuarioAutenticado principal = extrairPrincipal(jwt);

        logger.debug("JWT convertido para autenticação. userId: {}, empresaId: {}, roles: {}",
                principal.userId(), principal.empresaId(), authorities);

        // 3. Retorna o token de autenticação do Spring Security com o Principal e as authorities
        return new JwtAuthenticationToken(jwt, authorities, principal.userId().toString());
    }

    /**
     * Lê a claim "roles" do JWT e converte cada valor em um SimpleGrantedAuthority.
     * Exemplo: "ROLE_USER" → new SimpleGrantedAuthority("ROLE_USER")
     *
     * Se a claim não existir ou estiver vazia, retorna uma lista vazia (sem acesso).
     */
    private Collection<GrantedAuthority> extrairAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null || roles.isEmpty()) {
            logger.warn("Token JWT sem claim 'roles'. Nenhuma authority concedida.");
            return Collections.emptyList();
        }

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Lê as claims de contexto do JWT e monta o record UsuarioAutenticado.
     *
     * userId    → lido do campo padrão "sub" (subject) — retrocompatível com tokens antigos
     * empresaId → claim "empresaId" (Integer, pode ser null)
     * email     → claim "email" (String)
     *
     * Usar o "sub" em vez de uma claim customizada "userId" garante que tokens
     * emitidos antes dessa alteração (que não possuem a claim customizada) não
     * causem NullPointerException — resultando em 401, não em 500.
     */
    private UsuarioAutenticado extrairPrincipal(Jwt jwt) {
        // Lê do subject (campo padrão RFC 7519 — sempre presente em tokens válidos)
        String userIdStr = jwt.getSubject();
        String email     = jwt.getClaimAsString("email");
        Integer empresaId = null;

        // empresaId é opcional: usuário pode ainda não ter empresa cadastrada
        Object empresaIdClaim = jwt.getClaim("empresaId");
        if (empresaIdClaim instanceof Number) {
            empresaId = ((Number) empresaIdClaim).intValue();
        }

        UUID userId;
        try {
            if (userIdStr == null) {
                logger.error("Token JWT sem campo 'sub' (subject). Token inválido ou mal formado.");
                throw new IllegalArgumentException("Token JWT sem Subject.");
            }
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            logger.error("Falha ao extrair userId do subject do JWT: '{}'", userIdStr);
            throw new IllegalArgumentException("Token JWT com Subject inválido.");
        }

        return new UsuarioAutenticado(userId, empresaId, email);
    }
}
