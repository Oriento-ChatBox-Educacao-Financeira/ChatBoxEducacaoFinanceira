package com.oriento.api.services;

import com.oriento.api.dto.LoginRequest;
import com.oriento.api.dto.LoginResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
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

/**
 * Serviço responsável por gerenciar autenticação de usuários.
 * 
 * Funcionalidades principais:
 * - Validação de credenciais de login
 * - Gerenciamento de tentativas falhas de login
 * - Auditoria completa de acessos (sucessos e falhas)
 * - Geração de tokens JWT e refresh tokens
 * 
 * Este serviço implementa medidas de segurança como:
 * - Rastreamento de tentativas falhas por identificador (email/CNPJ)
 * - Logs detalhados de auditoria para compliance e segurança
 * - Mascaramento de dados sensíveis nos logs
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Repositório para buscar usuários no banco de dados
    private final UsuarioRepository usuarioRepository;
    
    // Encoder para validação de senhas usando BCrypt
    private final BCryptPasswordEncoder passwordEncoder;
    
    // Serviço para geração de tokens JWT
    private final JwtService jwtService;
    
    // Serviço para gerenciamento de refresh tokens
    private final RefreshTokenService refreshTokenService;

    /**
     * Mapa thread-safe para gerenciar tentativas falhas de login.
     * Chave: identificador do usuário (email ou CNPJ)
     * Valor: informações sobre as tentativas falhas
     * 
     * Usa ConcurrentHashMap para garantir thread-safety em ambientes multi-threaded
     */
    private final Map<String, FailedLoginAttempt> failedAttempts = new ConcurrentHashMap<>();

    /**
     * Construtor do serviço de autenticação.
     * 
     * @param usuarioRepository Repositório para acesso aos dados de usuários
     * @param passwordEncoder Encoder BCrypt para validação de senhas
     * @param jwtService Serviço para geração de tokens JWT
     * @param refreshTokenService Serviço para gerenciamento de refresh tokens
     */
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

    /**
     * Valida e processa o login do usuário
     * 
     * @param loginRequest Dados de login (email/CNPJ e senha)
     * @param clientIp Endereço IP do cliente para auditoria
     * @return LoginResponse com tokens de acesso
     * @throws BadCredentialsException se as credenciais forem inválidas
     */
    /**
     * Valida e processa o login do usuário.
     * 
     * Este método realiza as seguintes etapas:
     * 1. Identifica o usuário pelo email ou CNPJ
     * 2. Valida se os campos obrigatórios foram informados
     * 3. Busca o usuário no banco de dados
     * 4. Valida a senha usando BCrypt
     * 5. Registra auditoria de sucesso ou falha
     * 6. Gera tokens de acesso e refresh
     * 
     * @param loginRequest Dados de login contendo email/CNPJ e senha
     * @param clientIp Endereço IP do cliente para auditoria e segurança
     * @return LoginResponse contendo accessToken, refreshToken e tempo de expiração
     * @throws BadCredentialsException se as credenciais forem inválidas ou usuário não encontrado
     */
    public LoginResponse validarLogin(LoginRequest loginRequest, String clientIp) {
        logger.debug("Iniciando validação de login para IP: {}", clientIp);
        
        // Identifica o usuário pelo email ou CNPJ (o que foi informado)
        String identifier = StringUtils.hasText(loginRequest.email()) 
            ? loginRequest.email() 
            : loginRequest.cnpj();
        
        // Auditoria: Registra o início da tentativa de login
        auditarTentativaLogin(identifier, clientIp, "INICIADA");

        // ETAPA 1: Validação de entrada - Verifica se pelo menos um campo de identificação foi informado
        if (!StringUtils.hasText(loginRequest.email()) && !StringUtils.hasText(loginRequest.cnpj())) {
            logger.warn("Tentativa de login sem identificador (email ou CNPJ) do IP: {}", clientIp);
            registrarTentativaFalha(identifier, "Campo de identificação não informado", clientIp);
            throw new BadCredentialsException("Informe e-mail ou CNPJ para login!");
        }

        // ETAPA 2: Busca do usuário no banco de dados
        // Busca por email se informado, caso contrário busca por CNPJ
        Optional<Usuario> usuario = StringUtils.hasText(loginRequest.email())
                ? usuarioRepository.findByEmail(loginRequest.email())
                : usuarioRepository.findByCnpj(loginRequest.cnpj());

        // Verifica se o usuário existe no sistema
        if (usuario.isEmpty()) {
            logger.warn("Tentativa de login com usuário inexistente. Identificador: {}, IP: {}", 
                    maskIdentifier(identifier), clientIp);
            registrarTentativaFalha(identifier, "Usuário não encontrado", clientIp);
            throw new BadCredentialsException("Credenciais inválidas!");
        }

        logger.debug("Usuário encontrado. ID: {}, validando senha...", usuario.get().getIdUsuario());

        // ETAPA 3: Validação da senha usando BCrypt
        // A senha informada é comparada com o hash armazenado no banco
        if (!usuario.get().verificarLogin(loginRequest, passwordEncoder)) {
            logger.warn("Senha incorreta para usuário ID: {}, IP: {}", 
                    usuario.get().getIdUsuario(), clientIp);
            registrarTentativaFalha(identifier, "Senha incorreta", clientIp);
            throw new BadCredentialsException("Credenciais inválidas!");
        }

        // ETAPA 4: Login bem-sucedido
        // Limpa qualquer histórico de tentativas falhas anteriores
        limparTentativasFalhas(identifier);
        
        // Registra auditoria de sucesso
        auditarLoginSucesso(usuario.get(), clientIp);

        logger.info("Login validado com sucesso para usuário ID: {}", usuario.get().getIdUsuario());

        // ETAPA 5: Geração de tokens
        // Gera access token JWT com informações do usuário
        var accessToken = jwtService.gerarTokenJWT(usuario.get());
        
        // Cria refresh token para renovação do access token sem necessidade de novo login
        var refreshToken = refreshTokenService.criarRefreshToken(usuario.get().getIdUsuario());

        logger.debug("Tokens gerados com sucesso. Usuário ID: {}, AccessToken expira em {} segundos",
                usuario.get().getIdUsuario(), jwtService.getAccessTokenDuration());

        // Retorna resposta com os tokens gerados
        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenDuration()
        );
    }

    /**
     * Registra uma tentativa de login falha no sistema.
     * 
     * Este método mantém um histórico de tentativas falhas por identificador (email/CNPJ),
     * permitindo rastrear padrões suspeitos de tentativas de acesso não autorizado.
     * 
     * As informações armazenadas incluem:
     * - Número total de tentativas falhas consecutivas
     * - Data/hora da última tentativa
     * - Motivo da falha (senha incorreta, usuário não encontrado, etc)
     * - IP de origem da tentativa
     * 
     * @param identifier Email ou CNPJ do usuário que tentou fazer login
     * @param motivo Motivo específico da falha (ex: "Senha incorreta", "Usuário não encontrado")
     * @param clientIp Endereço IP do cliente que realizou a tentativa
     */
    private void registrarTentativaFalha(String identifier, String motivo, String clientIp) {
        // Obtém ou cria um registro de tentativas falhas para este identificador
        // computeIfAbsent garante thread-safety e cria novo registro se não existir
        FailedLoginAttempt attempt = failedAttempts.computeIfAbsent(
            identifier, 
            k -> {
                logger.debug("Criando novo registro de tentativas falhas para: {}", maskIdentifier(identifier));
                return new FailedLoginAttempt();
            }
        );
        
        // Incrementa o contador de tentativas falhas
        attempt.incrementarTentativas();
        
        // Atualiza informações da última tentativa
        attempt.setUltimaTentativa(LocalDateTime.now());
        attempt.setUltimoMotivo(motivo);
        attempt.setUltimoIp(clientIp);

        logger.debug("Tentativa falha registrada. Identificador: {}, Tentativas consecutivas: {}, Motivo: {}", 
                maskIdentifier(identifier), attempt.getTentativas(), motivo);

        // Registra na auditoria para análise posterior
        auditarTentativaFalha(identifier, motivo, clientIp, attempt.getTentativas());
    }

    /**
     * Limpa o histórico de tentativas falhas após um login bem-sucedido.
     * 
     * Quando um usuário consegue fazer login com sucesso, não há mais necessidade
     * de manter o histórico de tentativas falhas anteriores, pois o acesso foi autorizado.
     * 
     * @param identifier Email ou CNPJ do usuário que teve login bem-sucedido
     */
    private void limparTentativasFalhas(String identifier) {
        FailedLoginAttempt removed = failedAttempts.remove(identifier);
        if (removed != null) {
            logger.debug("Histórico de tentativas falhas limpo para: {} (tinha {} tentativas anteriores)", 
                    maskIdentifier(identifier), removed.getTentativas());
        }
    }

    /**
     * Obtém o número de tentativas falhas consecutivas para um identificador.
     * 
     * Útil para implementar bloqueios temporários ou alertas de segurança
     * quando há muitas tentativas falhas.
     * 
     * @param identifier Email ou CNPJ do usuário
     * @return Número de tentativas falhas consecutivas (0 se não houver histórico)
     */
    public int getTentativasFalhas(String identifier) {
        FailedLoginAttempt attempt = failedAttempts.get(identifier);
        int tentativas = attempt != null ? attempt.getTentativas() : 0;
        logger.debug("Consultando tentativas falhas para: {}. Total: {}", 
                maskIdentifier(identifier), tentativas);
        return tentativas;
    }

    /**
     * Obtém informações detalhadas sobre tentativas falhas de um identificador.
     * 
     * Retorna informações completas incluindo:
     * - Número de tentativas
     * - Data/hora da última tentativa
     * - Motivo da última falha
     * - IP de origem
     * 
     * @param identifier Email ou CNPJ do usuário
     * @return Objeto FailedLoginAttempt com informações detalhadas, ou null se não houver histórico
     */
    public FailedLoginAttempt getInformacoesTentativasFalhas(String identifier) {
        FailedLoginAttempt attempt = failedAttempts.get(identifier);
        if (attempt != null) {
            logger.debug("Consultando informações detalhadas de tentativas falhas para: {}", 
                    maskIdentifier(identifier));
        }
        return attempt;
    }

    /**
     * Auditoria: Registra tentativa de login iniciada
     */
    private void auditarTentativaLogin(String identifier, String clientIp, String status) {
        logger.info("[AUDITORIA] Tentativa de login {} - Identificador: {}, IP: {}, Timestamp: {}",
                status,
                maskIdentifier(identifier),
                clientIp,
                LocalDateTime.now());
    }

    /**
     * Auditoria: Registra login bem-sucedido
     */
    private void auditarLoginSucesso(Usuario usuario, String clientIp) {
        logger.info("[AUDITORIA] Login bem-sucedido - Usuário ID: {}, Email: {}, CNPJ: {}, IP: {}, Timestamp: {}",
                usuario.getIdUsuario(),
                maskEmail(usuario.getEmail()),
                maskCnpj(usuario.getCnpj()),
                clientIp,
                LocalDateTime.now());
    }

    /**
     * Auditoria: Registra tentativa de login falha
     */
    private void auditarTentativaFalha(String identifier, String motivo, String clientIp, int tentativas) {
        logger.warn("[AUDITORIA] Tentativa de login falha - Identificador: {}, Motivo: {}, IP: {}, Tentativas consecutivas: {}, Timestamp: {}",
                maskIdentifier(identifier),
                motivo,
                clientIp,
                tentativas,
                LocalDateTime.now());
    }

    /**
     * Mascara identificador (email ou CNPJ) para logs
     */
    private String maskIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) return "***";
        if (identifier.contains("@")) {
            return maskEmail(identifier);
        }
        return maskCnpj(identifier);
    }

    /**
     * Mascara email para logs
     */
    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) return "***@***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Mascara CNPJ para logs
     */
    private String maskCnpj(String cnpj) {
        if (!StringUtils.hasText(cnpj)) return "************";
        if (cnpj.length() < 4) return "************";
        return "********" + cnpj.substring(cnpj.length() - 4);
    }

    /**
     * Classe interna que armazena informações sobre tentativas falhas de login.
     * 
     * Esta classe mantém um registro das tentativas de acesso não autorizado,
     * permitindo identificar padrões suspeitos e implementar medidas de segurança
     * como bloqueios temporários ou alertas.
     * 
     * Campos armazenados:
     * - tentativas: contador de tentativas falhas consecutivas
     * - ultimaTentativa: timestamp da última tentativa falha
     * - ultimoMotivo: motivo da última falha (ex: "Senha incorreta")
     * - ultimoIp: IP de origem da última tentativa
     */
    public static class FailedLoginAttempt {
        /** Contador de tentativas falhas consecutivas */
        private int tentativas;
        
        /** Data e hora da última tentativa falha */
        private LocalDateTime ultimaTentativa;
        
        /** Motivo da última tentativa falha */
        private String ultimoMotivo;
        
        /** IP de origem da última tentativa */
        private String ultimoIp;

        /**
         * Incrementa o contador de tentativas falhas.
         * Chamado automaticamente a cada nova tentativa falha.
         */
        public void incrementarTentativas() {
            this.tentativas++;
        }

        /**
         * @return Número total de tentativas falhas consecutivas
         */
        public int getTentativas() {
            return tentativas;
        }

        /**
         * @return Data e hora da última tentativa falha
         */
        public LocalDateTime getUltimaTentativa() {
            return ultimaTentativa;
        }

        /**
         * Define a data e hora da última tentativa falha.
         * 
         * @param ultimaTentativa Timestamp da tentativa
         */
        public void setUltimaTentativa(LocalDateTime ultimaTentativa) {
            this.ultimaTentativa = ultimaTentativa;
        }

        /**
         * @return Motivo da última tentativa falha
         */
        public String getUltimoMotivo() {
            return ultimoMotivo;
        }

        /**
         * Define o motivo da última tentativa falha.
         * 
         * @param ultimoMotivo Motivo da falha (ex: "Senha incorreta", "Usuário não encontrado")
         */
        public void setUltimoMotivo(String ultimoMotivo) {
            this.ultimoMotivo = ultimoMotivo;
        }

        /**
         * @return IP de origem da última tentativa
         */
        public String getUltimoIp() {
            return ultimoIp;
        }

        /**
         * Define o IP de origem da última tentativa.
         * 
         * @param ultimoIp Endereço IP do cliente
         */
        public void setUltimoIp(String ultimoIp) {
            this.ultimoIp = ultimoIp;
        }
    }
}
