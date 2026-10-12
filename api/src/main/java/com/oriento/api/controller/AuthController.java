package com.oriento.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oriento.api.dto.LoginRequest;
import com.oriento.api.dto.LoginResponse;
import com.oriento.api.dto.RefreshTokenDTO;
import com.oriento.api.dto.UsuarioResponse;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.RefreshToken;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.services.AuthService;
import com.oriento.api.services.JwtService;
import com.oriento.api.services.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmpresaRepository empresaRepository;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          EmpresaRepository empresaRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.empresaRepository = empresaRepository;

        logger.info("AuthController inicializado com sucesso");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        // X-Forwarded-For pode conter múltiplos IPs separados por vírgula
        // O primeiro IP é sempre o IP original do cliente
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String ip = xForwardedFor.split(",")[0].trim();
            logger.debug("IP obtido do header X-Forwarded-For: {}", ip);
            return ip;
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            logger.debug("IP obtido do header X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddr();
        logger.debug("IP obtido do RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest,
                                               HttpServletRequest request) {
        logger.info("Recebida requisição de login");

        String clientIp = getClientIpAddress(request);
        logger.debug("IP do cliente identificado: {}", clientIp);

        LoginResponse response = authService.validarLogin(loginRequest, clientIp);
        
        logger.info("Login processado com sucesso");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        logger.info("Recebida requisição de refresh token");

        RefreshToken refreshToken = refreshTokenService.validarRefreshToken(refreshTokenDTO.refreshToken());
        Usuario usuario = refreshToken.getUsuario();

        logger.debug("Refresh token válido. Gerando novo access token para usuário ID: {}",
                usuario.getIdUsuario());

        // Busca empresa para manter as claims atualizadas no novo token
        Empresa empresa = empresaRepository.findByUsuario(usuario).orElse(null);

        var novoAccessToken = jwtService.gerarTokenJWT(usuario, empresa);

        logger.info("Novo access token gerado com sucesso para usuário ID: {}", usuario.getIdUsuario());

        return ResponseEntity.ok(new LoginResponse(
                novoAccessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenDuration(),
                UsuarioResponse.fromEntity(usuario)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        if (refreshTokenDTO == null || refreshTokenDTO.refreshToken() == null || refreshTokenDTO.refreshToken().isBlank()) {
            logger.warn("Tentativa de logout sem refresh token");
            return ResponseEntity.badRequest().build();
        }

        logger.info("Recebida requisição de logout");
        refreshTokenService.deletarRefreshToken(refreshTokenDTO.refreshToken());
        return ResponseEntity.noContent().build();
    }

}
