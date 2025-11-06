package com.oriento.api.services;

import com.oriento.api.model.RefreshToken;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.RefreshTokenRepository;
import com.oriento.api.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de refresh tokens.
 * 
 * Refresh tokens são tokens de longa duração usados para renovar access tokens
 * sem necessidade de fazer login novamente. Eles têm vida útil maior (15 dias)
 * e são armazenados no banco de dados.
 * 
 * Funcionalidades:
 * - Criação de refresh tokens para usuários
 * - Validação de refresh tokens (verifica existência e expiração)
 * - Remoção de refresh tokens (logout)
 * - Gerenciamento automático de tokens antigos (substitui token anterior ao criar novo)
 * 
 * Fluxo de uso:
 * 1. Usuário faz login → recebe access token (15 min) + refresh token (15 dias)
 * 2. Access token expira → cliente usa refresh token para obter novo access token
 * 3. Refresh token expira → usuário precisa fazer login novamente
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    /**
     * Repositório para acesso aos dados de refresh tokens no banco de dados.
     */
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Repositório para acesso aos dados de usuários no banco de dados.
     */
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Duração do refresh token em segundos.
     * 
     * Valor: 1.296.000 segundos = 15 dias
     * 
     * Refresh tokens têm vida útil maior que access tokens para permitir
     * que usuários permaneçam autenticados por períodos mais longos sem
     * precisar fazer login novamente.
     */
    private static final long REFRESH_TOKEN_DURATION = 1296000L; // 15 * 24 * 60 * 60 segundos

    /**
     * Cria um novo refresh token para o usuário especificado.
     * 
     * Este método:
     * 1. Busca o usuário no banco de dados
     * 2. Remove qualquer refresh token anterior do usuário (um usuário tem apenas um token ativo)
     * 3. Cria um novo refresh token com UUID aleatório
     * 4. Define data de expiração (15 dias a partir de agora)
     * 5. Salva no banco de dados
     * 
     * O método é transacional, garantindo que todas as operações sejam executadas
     * atomicamente (ou todas com sucesso ou nenhuma).
     * 
     * @param userId ID do usuário (UUID) para o qual o refresh token será criado
     * @return RefreshToken criado e salvo no banco de dados
     * @throws RuntimeException se o usuário não for encontrado
     */
    @Transactional
    public RefreshToken criarRefreshToken(UUID userId) {
        logger.debug("Criando refresh token para usuário ID: {}", userId);
        
        RefreshToken refreshToken = new RefreshToken();
        
        // Busca o usuário no banco de dados
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Tentativa de criar refresh token para usuário inexistente: {}", userId);
                    return new RuntimeException("Usuário não encontrado");
                });
        
        logger.debug("Usuário encontrado. Configurando refresh token...");
        
        // Configura o refresh token
        refreshToken.setUsuario(usuario);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_DURATION));
        refreshToken.setToken(UUID.randomUUID().toString()); // Gera token único
        
        // Remove token antigo se existir (um usuário tem apenas um token ativo por vez)
        refreshTokenRepository.findByUsuario(usuario).ifPresent(oldToken -> {
            logger.debug("Removendo refresh token antigo do usuário ID: {}", userId);
            refreshTokenRepository.delete(oldToken);
        });
        
        // Salva o novo refresh token no banco de dados
        refreshToken = refreshTokenRepository.save(refreshToken);
        
        logger.info("Refresh token criado com sucesso para usuário ID: {}. Expira em: {}", 
                userId, refreshToken.getExpiryDate());
        
        return refreshToken;
    }

    /**
     * Valida um refresh token, verificando se existe e não está expirado.
     * 
     * Este método:
     * 1. Busca o refresh token no banco de dados pelo token fornecido
     * 2. Verifica se o token existe
     * 3. Verifica se o token não está expirado (data de expiração > agora)
     * 4. Retorna o token se válido, ou lança exceção se inválido/expirado
     * 
     * @param token String do refresh token a ser validado
     * @return RefreshToken válido e não expirado
     * @throws RuntimeException se o token não for encontrado ou estiver expirado
     */
    public RefreshToken validarRefreshToken(String token) {
        logger.debug("Validando refresh token");
        
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .filter(this::verificarExpiracao)
                .orElseThrow(() -> {
                    logger.warn("Tentativa de usar refresh token inválido ou expirado");
                    return new RuntimeException("Refresh Token inválido ou expirado");
                });
        
        logger.debug("Refresh token válido. Usuário ID: {}", refreshToken.getUsuario().getIdUsuario());
        return refreshToken;
    }

    /**
     * Remove (deleta) um refresh token do banco de dados.
     * 
     * Útil para implementar logout, onde o refresh token é invalidado
     * para que não possa mais ser usado para renovar access tokens.
     * 
     * O método é transacional, garantindo que a operação seja atômica.
     * 
     * @param token String do refresh token a ser deletado
     */
    @Transactional
    public void deletarRefreshToken(String token) {
        logger.debug("Deletando refresh token");
        
        refreshTokenRepository.findByToken(token)
                .ifPresentOrElse(
                    refreshToken -> {
                        refreshTokenRepository.delete(refreshToken);
                        logger.info("Refresh token deletado com sucesso. Usuário ID: {}", 
                                refreshToken.getUsuario().getIdUsuario());
                    },
                    () -> logger.warn("Tentativa de deletar refresh token inexistente")
                );
    }

    /**
     * Verifica se um refresh token ainda não expirou.
     * 
     * Compara a data de expiração do token com o momento atual.
     * 
     * @param token RefreshToken a ser verificado
     * @return true se o token ainda não expirou (data de expiração > agora), false caso contrário
     */
    private boolean verificarExpiracao(RefreshToken token) {
        boolean isValid = token.getExpiryDate().compareTo(Instant.now()) > 0;
        
        if (!isValid) {
            logger.debug("Refresh token expirado. Data de expiração: {}, Agora: {}", 
                    token.getExpiryDate(), Instant.now());
        }
        
        return isValid;
    }
}

