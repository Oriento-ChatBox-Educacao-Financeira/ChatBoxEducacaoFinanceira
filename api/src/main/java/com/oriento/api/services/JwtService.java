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

/**
 * Serviço responsável pela geração de tokens JWT (JSON Web Tokens).
 * 
 * Este serviço utiliza a biblioteca JJWT para criar tokens JWT assinados com RSA,
 * que são usados para autenticação e autorização na API.
 * 
 * Funcionalidades:
 * - Geração de access tokens JWT assinados com chave privada RSA
 * - Configuração de expiração de tokens (15 minutos por padrão)
 * - Inclusão de informações do usuário no token (subject = ID do usuário)
 * 
 * Os tokens gerados contêm:
 * - Issuer: "oriento" (identificador da aplicação)
 * - Subject: ID do usuário (UUID)
 * - Issued At: Data/hora de criação
 * - Expiration: Data/hora de expiração (15 minutos após criação)
 * - Assinatura: RSA usando chave privada
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    /**
     * Chave privada RSA usada para assinar os tokens JWT.
     * A chave pública correspondente é usada para validar os tokens.
     */
    @Autowired
    private RSAPrivateKey privateKey;

    /**
     * Duração do access token em segundos.
     * 
     * Valor: 900 segundos = 15 minutos
     * 
     * Este é um valor de segurança recomendado que equilibra:
     * - Segurança: Tokens com vida curta reduzem o risco em caso de comprometimento
     * - Usabilidade: Tempo suficiente para uso normal sem necessidade constante de refresh
     * 
     * Para renovar o token sem fazer login novamente, use o refresh token.
     */
    private static final long ACCESS_TOKEN_DURATION = 900L; // 15 * 60 segundos

    /**
     * Gera um token JWT para o usuário especificado.
     * 
     * O token contém:
     * - Issuer: "oriento" (identificador da aplicação)
     * - Subject: ID do usuário (UUID convertido para String)
     * - Issued At: Momento atual
     * - Expiration: Momento atual + duração do token (15 minutos)
     * - Assinatura: RSA usando a chave privada
     * 
     * O token é compactado em uma String Base64 URL-safe que pode ser enviada
     * no header Authorization das requisições HTTP.
     * 
     * @param usuario Usuário para o qual o token será gerado
     * @return String contendo o token JWT compactado
     */
    public String gerarTokenJWT(Usuario usuario) {
        logger.debug("Gerando token JWT para usuário ID: {}", usuario.getIdUsuario());
        
        var now = Instant.now();
        var expiration = now.plusSeconds(ACCESS_TOKEN_DURATION);

        // Constrói o token JWT com todas as informações necessárias
        var token = Jwts.builder()
                .issuer("oriento") // Identificador da aplicação que emitiu o token
                .subject(usuario.getIdUsuario().toString()) // ID do usuário como subject
                .issuedAt(Date.from(now)) // Data/hora de criação
                .expiration(Date.from(expiration)) // Data/hora de expiração
                .signWith(privateKey) // Assina com chave privada RSA
                .compact(); // Compacta em String Base64 URL-safe

        logger.debug("Token JWT gerado com sucesso para usuário ID: {}. Expira em: {}", 
                usuario.getIdUsuario(), expiration);
        
        return token;
    }

    /**
     * Retorna a duração do access token em segundos.
     * 
     * Útil para incluir na resposta de login, permitindo que o cliente
     * saiba quando o token expirará e precise ser renovado.
     * 
     * @return Duração do token em segundos (900 = 15 minutos)
     */
    public Long getAccessTokenDuration() {
        return ACCESS_TOKEN_DURATION;
    }

}
