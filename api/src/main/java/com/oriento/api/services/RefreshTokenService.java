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

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final long REFRESH_TOKEN_DURATION = 1296000L;

    @Transactional
    public RefreshToken criarRefreshToken(UUID userId) {
        logger.debug("Criando refresh token para usuário ID: {}", userId);
        
        RefreshToken refreshToken = new RefreshToken();

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Tentativa de criar refresh token para usuário inexistente: {}", userId);
                    return new RuntimeException("Usuário não encontrado");
                });
        
        logger.debug("Usuário encontrado. Configurando refresh token...");

        refreshToken.setUsuario(usuario);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(REFRESH_TOKEN_DURATION));
        refreshToken.setToken(UUID.randomUUID().toString());

        // Remove qualquer refresh token anterior do usuário (pode haver mais
        // de um se houver corrida de logins ou estado inconsistente do banco).
        // Usa delete em bulk + flush para evitar conflitar com o save abaixo
        // dentro da mesma transação.
        int removidos = refreshTokenRepository.deleteAllByUsuario(usuario);
        if (removidos > 0) {
            logger.debug("Removidos {} refresh token(s) antigo(s) do usuário ID: {}", removidos, userId);
            refreshTokenRepository.flush();
        }

        refreshToken = refreshTokenRepository.save(refreshToken);
        
        logger.info("Refresh token criado com sucesso para usuário ID: {}. Expira em: {}", 
                userId, refreshToken.getExpiryDate());
        
        return refreshToken;
    }

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

    private boolean verificarExpiracao(RefreshToken token) {
        boolean isValid = token.getExpiryDate().compareTo(Instant.now()) > 0;
        
        if (!isValid) {
            logger.debug("Refresh token expirado. Data de expiração: {}, Agora: {}", 
                    token.getExpiryDate(), Instant.now());
        }
        
        return isValid;
    }
}

