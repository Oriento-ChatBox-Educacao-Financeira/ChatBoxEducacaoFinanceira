package com.oriento.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oriento.api.dto.AlterarSenhaDTO;
import com.oriento.api.dto.LoginRequest;
import com.oriento.api.dto.LoginResponse;
import com.oriento.api.dto.RefreshTokenDTO;
import com.oriento.api.dto.UsuarioResponse;
import com.oriento.api.model.RefreshToken;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import com.oriento.api.services.AuthService;
import com.oriento.api.services.CurrentUserService;
import com.oriento.api.services.JwtService;
import com.oriento.api.services.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserService currentUserService;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          CurrentUserService currentUserService,
                          UsuarioRepository usuarioRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.currentUserService = currentUserService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;

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

        var novoAccessToken = jwtService.gerarTokenJWT(usuario);
        
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

    /**
     * Retorna o usuário autenticado (e a empresa associada). Usado pelo
     * frontend para hidratar a sessão no boot ou após refresh do token.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        Usuario usuario = currentUserService.obterUsuarioAutenticado();
        return ResponseEntity.ok(UsuarioResponse.fromEntity(usuario));
    }

    /**
     * Altera a senha do usuário autenticado. Não exige a senha atual (cenário
     * de "usuário logado já tem prova de identidade"); para o fluxo de senha
     * esquecida ainda não há endpoint dedicado.
     */
    @PatchMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody AlterarSenhaDTO dto) {
        Usuario usuario = currentUserService.obterUsuarioAutenticado();
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
        logger.info("Senha atualizada para usuario ID: {}", usuario.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}
