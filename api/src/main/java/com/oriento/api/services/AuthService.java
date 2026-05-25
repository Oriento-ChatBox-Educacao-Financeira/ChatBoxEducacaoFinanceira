package com.oriento.api.services;

import com.oriento.api.dto.LoginRequest;
import com.oriento.api.dto.LoginResponse;
import com.oriento.api.dto.UsuarioResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final Map<String, FailedLoginAttempt> failedAttempts = new ConcurrentHashMap<>();

    public AuthService(UsuarioRepository usuarioRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        
        logger.debug("AuthService inicializado com sucesso");
    }

    public LoginResponse validarLogin(LoginRequest loginRequest, String clientIp) {
        logger.debug("Iniciando validação de login para IP: {}", clientIp);

        String identifier = StringUtils.hasText(loginRequest.email()) 
            ? loginRequest.email() 
            : loginRequest.cnpj();

        auditarTentativaLogin(identifier, clientIp, "INICIADA");

        if (!StringUtils.hasText(loginRequest.email()) && !StringUtils.hasText(loginRequest.cnpj())) {
            logger.warn("Tentativa de login sem identificador (email ou CNPJ) do IP: {}", clientIp);
            registrarTentativaFalha(identifier, "Campo de identificação não informado", clientIp);
            throw new BadCredentialsException("Informe e-mail ou CNPJ para login!");
        }

        Optional<Usuario> usuario = StringUtils.hasText(loginRequest.email())
                ? usuarioRepository.findByEmail(loginRequest.email())
                : usuarioRepository.findByCnpj(loginRequest.cnpj());

        if (usuario.isEmpty()) {
            logger.warn("Tentativa de login com usuário inexistente. Identificador: {}, IP: {}", 
                    maskIdentifier(identifier), clientIp);
            registrarTentativaFalha(identifier, "Usuário não encontrado", clientIp);
            throw new BadCredentialsException("Credenciais inválidas!");
        }

        logger.debug("Usuário encontrado. ID: {}, validando senha...", usuario.get().getIdUsuario());

        if (!usuario.get().verificarLogin(loginRequest, passwordEncoder)) {
            logger.warn("Senha incorreta para usuário ID: {}, IP: {}", 
                    usuario.get().getIdUsuario(), clientIp);
            registrarTentativaFalha(identifier, "Senha incorreta", clientIp);
            throw new BadCredentialsException("Credenciais inválidas!");
        }

        limparTentativasFalhas(identifier);
        auditarLoginSucesso(usuario.get(), clientIp);

        logger.info("Login validado com sucesso para usuário ID: {}", usuario.get().getIdUsuario());

        var accessToken = jwtService.gerarTokenJWT(usuario.get());

        var refreshToken = refreshTokenService.criarRefreshToken(usuario.get().getIdUsuario());

        logger.debug("Tokens gerados com sucesso. Usuário ID: {}, AccessToken expira em {} segundos",
                usuario.get().getIdUsuario(), jwtService.getAccessTokenDuration());

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenDuration(),
                UsuarioResponse.fromEntity(usuario.get())
        );
    }

    private void registrarTentativaFalha(String identifier, String motivo, String clientIp) {
        FailedLoginAttempt attempt = failedAttempts.computeIfAbsent(
            identifier, 
            k -> {
                logger.debug("Criando novo registro de tentativas falhas para: {}", maskIdentifier(identifier));
                return new FailedLoginAttempt();
            }
        );

        attempt.incrementarTentativas();

        attempt.setUltimaTentativa(LocalDateTime.now());
        attempt.setUltimoMotivo(motivo);
        attempt.setUltimoIp(clientIp);

        logger.debug("Tentativa falha registrada. Identificador: {}, Tentativas consecutivas: {}, Motivo: {}", 
                maskIdentifier(identifier), attempt.getTentativas(), motivo);

        auditarTentativaFalha(identifier, motivo, clientIp, attempt.getTentativas());
    }

    private void limparTentativasFalhas(String identifier) {
        FailedLoginAttempt removed = failedAttempts.remove(identifier);
        if (removed != null) {
            logger.debug("Histórico de tentativas falhas limpo para: {} (tinha {} tentativas anteriores)", 
                    maskIdentifier(identifier), removed.getTentativas());
        }
    }

    public int getTentativasFalhas(String identifier) {
        FailedLoginAttempt attempt = failedAttempts.get(identifier);
        int tentativas = attempt != null ? attempt.getTentativas() : 0;
        logger.debug("Consultando tentativas falhas para: {}. Total: {}", 
                maskIdentifier(identifier), tentativas);
        return tentativas;
    }

    public FailedLoginAttempt getInformacoesTentativasFalhas(String identifier) {
        FailedLoginAttempt attempt = failedAttempts.get(identifier);
        if (attempt != null) {
            logger.debug("Consultando informações detalhadas de tentativas falhas para: {}", 
                    maskIdentifier(identifier));
        }
        return attempt;
    }

    private void auditarTentativaLogin(String identifier, String clientIp, String status) {
        logger.info("[AUDITORIA] Tentativa de login {} - Identificador: {}, IP: {}, Timestamp: {}",
                status,
                maskIdentifier(identifier),
                clientIp,
                LocalDateTime.now());
    }

    private void auditarLoginSucesso(@NonNull Usuario usuario, String clientIp) {
        logger.info("[AUDITORIA] Login bem-sucedido - Usuário ID: {}, Email: {}, CNPJ: {}, IP: {}, Timestamp: {}",
                usuario.getIdUsuario(),
                maskEmail(usuario.getEmail()),
                maskCnpj(usuario.getCnpj()),
                clientIp,
                LocalDateTime.now());
    }

    private void auditarTentativaFalha(String identifier, String motivo, String clientIp, int tentativas) {
        logger.warn("[AUDITORIA] Tentativa de login falha - Identificador: {}, Motivo: {}, IP: {}, Tentativas consecutivas: {}, Timestamp: {}",
                maskIdentifier(identifier),
                motivo,
                clientIp,
                tentativas,
                LocalDateTime.now());
    }

    private String maskIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) return "***";
        if (identifier.contains("@")) {
            return maskEmail(identifier);
        }
        return maskCnpj(identifier);
    }

    private @NonNull String maskEmail(String email) {
        if (!StringUtils.hasText(email)) return "***@***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private @NonNull String maskCnpj(String cnpj) {
        if (!StringUtils.hasText(cnpj)) return "************";
        if (cnpj.length() < 4) return "************";
        return "********" + cnpj.substring(cnpj.length() - 4);
    }

    public static class FailedLoginAttempt {
        private int tentativas;
        private LocalDateTime ultimaTentativa;
        private String ultimoMotivo;
        private String ultimoIp;
        public void incrementarTentativas() {
            this.tentativas++;
        }
        public int getTentativas() {
            return tentativas;
        }

        public LocalDateTime getUltimaTentativa() {
            return ultimaTentativa;
        }
        public void setUltimaTentativa(LocalDateTime ultimaTentativa) {
            this.ultimaTentativa = ultimaTentativa;
        }
        public String getUltimoMotivo() {
            return ultimoMotivo;
        }
        public void setUltimoMotivo(String ultimoMotivo) {
            this.ultimoMotivo = ultimoMotivo;
        }
        public String getUltimoIp() {
            return ultimoIp;
        }
        public void setUltimoIp(String ultimoIp) {
            this.ultimoIp = ultimoIp;
        }
    }
}
