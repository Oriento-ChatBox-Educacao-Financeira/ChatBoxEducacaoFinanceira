package com.oriento.api.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Configuração de segurança e autenticação da aplicação.
 * 
 * Esta classe configura:
 * - Spring Security: regras de autorização e autenticação
 * - JWT: codificação e decodificação de tokens JWT usando RSA
 * - BCrypt: encoder para criptografia de senhas
 * - CORS: configuração de Cross-Origin Resource Sharing
 * - Sessões: política stateless (sem sessões HTTP)
 * 
 * Endpoints públicos (não requerem autenticação):
 * - POST /login: Autenticação de usuários
 * - POST /cadastro: Cadastro de novos usuários
 * - POST /refresh: Renovação de access tokens
 * 
 * Todos os outros endpoints requerem autenticação via JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AuthConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuthConfig.class);

    /**
     * Chave pública RSA usada para validar tokens JWT.
     * Carregada do arquivo de chaves via RsaKeyLoader.
     */
    @Autowired
    private RSAPublicKey publicKey;
    
    /**
     * Chave privada RSA usada para assinar tokens JWT.
     * Carregada do arquivo de chaves via RsaKeyLoader.
     */
    @Autowired
    private RSAPrivateKey privateKey;

    /**
     * Configura a cadeia de filtros de segurança do Spring Security.
     * 
     * Define:
     * - CORS habilitado com configurações padrão
     * - CSRF desabilitado (não necessário para APIs stateless com JWT)
     * - Endpoints públicos: /login, /cadastro, /refresh
     * - Todos os outros endpoints requerem autenticação
     * - OAuth2 Resource Server com JWT como método de autenticação
     * - Sessões stateless (sem sessões HTTP, usa apenas JWT)
     * 
     * @param http Objeto HttpSecurity para configurar a segurança
     * @return SecurityFilterChain configurado
     * @throws Exception se ocorrer erro na configuração
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configurando SecurityFilterChain...");
        
        http
                // Habilita CORS com configurações padrão
                .cors(Customizer.withDefaults())
                
                // Desabilita CSRF (não necessário para APIs stateless com JWT)
                .csrf(AbstractHttpConfigurer::disable)
                
                // Configura regras de autorização
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints públicos (não requerem autenticação)
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cadastro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/refresh").permitAll()
                        
                        // Todos os outros endpoints requerem autenticação
                        .anyRequest().authenticated())
                
                // Configura OAuth2 Resource Server com JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                
                // Configura política de sessões como stateless (sem sessões HTTP)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        logger.info("SecurityFilterChain configurado com sucesso");
        return http.build();
    }

    /**
     * Cria o encoder JWT usado para gerar tokens JWT.
     * 
     * O encoder usa a chave privada RSA para assinar os tokens.
     * Utiliza a biblioteca Nimbus JOSE + JWT para implementação.
     * 
     * @return JwtEncoder configurado com chave privada RSA
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        logger.debug("Criando JwtEncoder com chave privada RSA");
        
        // Cria uma chave JWK (JSON Web Key) a partir das chaves RSA
        JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(privateKey).build();
        
        // Cria um conjunto de chaves JWK (JWKSet) imutável
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        
        // Cria o encoder Nimbus usando o JWKSet
        return new NimbusJwtEncoder(jwks);
    }

    /**
     * Cria o decoder JWT usado para validar e decodificar tokens JWT.
     * 
     * O decoder usa a chave pública RSA para validar a assinatura dos tokens.
     * Utiliza a biblioteca Nimbus JOSE + JWT para implementação.
     * 
     * @return JwtDecoder configurado com chave pública RSA
     */
    @Bean
    public JwtDecoder jwtDencoder() {
        logger.debug("Criando JwtDecoder com chave pública RSA");
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    /**
     * Cria o encoder BCrypt usado para criptografar senhas.
     * 
     * BCrypt é um algoritmo de hash de senha projetado especificamente para
     * armazenamento seguro de senhas. Características:
     * - Gera hash único a cada execução (mesmo com mesma senha)
     * - Inclui salt automático
     * - Resistente a ataques de força bruta
     * - Configurável em termos de complexidade (rounds)
     * 
     * @return BCryptPasswordEncoder configurado
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        logger.debug("Criando BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }

}
