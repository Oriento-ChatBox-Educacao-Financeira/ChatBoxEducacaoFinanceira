package com.oriento.api.controller;

import com.oriento.api.dto.LoginRequest;
import com.oriento.api.dto.LoginResponse;
import com.oriento.api.dto.RefreshTokenDTO;
import com.oriento.api.model.RefreshToken;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.AuthService;
import com.oriento.api.services.JwtService;
import com.oriento.api.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável por gerenciar endpoints de autenticação.
 * 
 * Endpoints disponíveis:
 * - POST /login: Autentica um usuário e retorna tokens JWT
 * - POST /refresh: Renova o access token usando um refresh token válido
 * 
 * Este controller atua como uma camada fina, delegando toda a lógica de negócio
 * para o AuthService, mantendo a separação de responsabilidades.
 */
@RestController
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Serviço de autenticação que contém toda a lógica de validação e geração de tokens
    private final AuthService authService;
    
    // Serviço para geração de tokens JWT
    private final JwtService jwtService;
    
    // Serviço para gerenciamento de refresh tokens
    private final RefreshTokenService refreshTokenService;

    /**
     * Construtor do controller de autenticação.
     * 
     * @param authService Serviço que contém a lógica de autenticação
     * @param jwtService Serviço para geração de tokens JWT
     * @param refreshTokenService Serviço para gerenciamento de refresh tokens
     */
    public AuthController(AuthService authService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        
        logger.info("AuthController inicializado com sucesso");
    }

    /**
     * Obtém o endereço IP real do cliente, considerando proxies e load balancers.
     * 
     * Em ambientes com proxies reversos ou load balancers, o IP do cliente pode
     * estar em headers HTTP específicos. Este método verifica na seguinte ordem:
     * 1. Header X-Forwarded-For (padrão em proxies)
     * 2. Header X-Real-IP (usado por alguns load balancers)
     * 3. RemoteAddr do request (IP direto quando não há proxy)
     * 
     * @param request Objeto HttpServletRequest contendo informações da requisição
     * @return Endereço IP real do cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // X-Forwarded-For pode conter múltiplos IPs separados por vírgula
        // O primeiro IP é sempre o IP original do cliente
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String ip = xForwardedFor.split(",")[0].trim();
            logger.debug("IP obtido do header X-Forwarded-For: {}", ip);
            return ip;
        }
        
        // X-Real-IP é usado por alguns load balancers (ex: Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            logger.debug("IP obtido do header X-Real-IP: {}", xRealIp);
            return xRealIp;
        }
        
        // Fallback: IP direto da conexão (quando não há proxy)
        String remoteAddr = request.getRemoteAddr();
        logger.debug("IP obtido do RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Endpoint para autenticação de usuários.
     * 
     * Este endpoint recebe as credenciais do usuário (email/CNPJ e senha),
     * valida as credenciais através do AuthService e retorna tokens JWT
     * para acesso à API.
     * 
     * O IP do cliente é extraído automaticamente para auditoria e segurança.
     * 
     * @param loginRequest DTO contendo email/CNPJ e senha do usuário
     * @param request Objeto HttpServletRequest para extrair informações da requisição
     * @return ResponseEntity com LoginResponse contendo accessToken, refreshToken e tempo de expiração
     * @throws BadCredentialsException se as credenciais forem inválidas (tratado pelo Spring)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest,
                                               HttpServletRequest request) {
        logger.info("Recebida requisição de login");
        
        // Extrai o IP real do cliente para auditoria e segurança
        String clientIp = getClientIpAddress(request);
        logger.debug("IP do cliente identificado: {}", clientIp);
        
        // Delega a validação e geração de tokens para o AuthService
        // O AuthService é responsável por toda a lógica de negócio
        LoginResponse response = authService.validarLogin(loginRequest, clientIp);
        
        logger.info("Login processado com sucesso");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para renovação de access token usando refresh token.
     * 
     * Quando um access token expira, o cliente pode usar este endpoint
     * para obter um novo access token sem precisar fazer login novamente,
     * desde que o refresh token ainda seja válido.
     * 
     * Fluxo:
     * 1. Valida o refresh token fornecido
     * 2. Busca o usuário associado ao refresh token
     * 3. Gera um novo access token para o usuário
     * 4. Retorna o novo access token junto com o refresh token (mantido)
     * 
     * @param refreshTokenDTO DTO contendo o refresh token
     * @return ResponseEntity com LoginResponse contendo novo accessToken, refreshToken e tempo de expiração
     * @throws Exception se o refresh token for inválido ou expirado (tratado pelo Spring)
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        logger.info("Recebida requisição de refresh token");
        
        // Valida o refresh token e obtém o token associado
        RefreshToken refreshToken = refreshTokenService.validarRefreshToken(refreshTokenDTO.refreshToken());
        Usuario usuario = refreshToken.getUsuario();
        
        logger.debug("Refresh token válido. Gerando novo access token para usuário ID: {}", 
                usuario.getIdUsuario());
        
        // Gera um novo access token JWT para o usuário
        var novoAccessToken = jwtService.gerarTokenJWT(usuario);
        
        logger.info("Novo access token gerado com sucesso para usuário ID: {}", usuario.getIdUsuario());
        
        // Retorna o novo access token junto com o refresh token (que permanece o mesmo)
        return ResponseEntity.ok(new LoginResponse(
                novoAccessToken, 
                refreshToken.getToken(), 
                jwtService.getAccessTokenDuration()
        ));
    }

}
