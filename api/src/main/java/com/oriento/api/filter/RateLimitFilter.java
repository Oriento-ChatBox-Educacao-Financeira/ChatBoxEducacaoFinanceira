package com.oriento.api.filter;

import com.oriento.api.exception.RateLimitExceededException;
import com.oriento.api.services.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP que implementa Rate Limiting usando Bucket4j.
 * 
 * Este filtro intercepta todas as requisições HTTP antes que cheguem aos controllers,
 * aplicando limites de taxa de requisições para proteger a API contra:
 * - Ataques de força bruta (especialmente no endpoint de login)
 * - Sobrecarga de requisições (DDoS)
 * - Uso excessivo de recursos
 * 
 * Funcionalidades:
 * - Rate limit específico para endpoint de login (5 tentativas por 15 minutos)
 * - Rate limit padrão para outros endpoints (configurável)
 * - Identificação de clientes por IP (considera proxies/load balancers)
 * - Respostas HTTP 429 (Too Many Requests) com informações de retry
 * 
 * O filtro é executado uma vez por requisição (OncePerRequestFilter) e é
 * registrado automaticamente pelo Spring Boot devido à anotação @Component.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    // Serviço que gerencia os buckets de rate limiting
    private final RateLimitService rateLimitService;

    /**
     * Capacidade máxima do bucket para endpoint de login.
     * Define quantas tentativas de login são permitidas antes de bloquear.
     * Valor padrão: 5 tentativas
     */
    @Value("${ratelimit.login.capacity:5}")
    private long loginCapacity;

    /**
     * Quantidade de tokens a serem repostos no bucket de login.
     * Geralmente igual à capacidade.
     * Valor padrão: 5 tokens
     */
    @Value("${ratelimit.login.refill.tokens:5}")
    private long loginRefillTokens;

    /**
     * Duração em segundos para repor os tokens no bucket de login.
     * Valor padrão: 900 segundos (15 minutos)
     */
    @Value("${ratelimit.login.refill.duration:900}")
    private long loginRefillDuration;

    /**
     * Construtor do filtro de rate limiting.
     * 
     * @param rateLimitService Serviço que gerencia os buckets de rate limiting
     */
    @Autowired
    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        logger.info("RateLimitFilter inicializado. Login limit: {} tentativas a cada {} segundos", 
                loginCapacity, loginRefillDuration);
    }

    /**
     * Método principal do filtro que intercepta todas as requisições HTTP.
     * 
     * Este método é chamado uma vez por requisição e realiza as seguintes etapas:
     * 1. Identifica o cliente (por IP) e o endpoint acessado
     * 2. Verifica se é uma requisição de login (aplica rate limit específico)
     * 3. Tenta consumir um token do bucket apropriado
     * 4. Se permitido, continua a cadeia de filtros (requisição prossegue)
     * 5. Se bloqueado, retorna HTTP 429 com informações de retry
     * 
     * @param request Objeto HttpServletRequest contendo informações da requisição
     * @param response Objeto HttpServletResponse para enviar resposta ao cliente
     * @param filterChain Cadeia de filtros do Spring (para continuar o processamento)
     * @throws ServletException se ocorrer erro no processamento do servlet
     * @throws IOException se ocorrer erro de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // Gera uma chave única para identificar o cliente e o endpoint
            // Formato: "IP:endpoint" (ex: "192.168.1.1:/login")
            String clientKey = getClientKey(request);
            String requestPath = request.getRequestURI();
            boolean isLoginRequest = "/login".equals(requestPath);
            
            logger.debug("Processando rate limit. Endpoint: {}, IP: {}, Chave: {}", 
                    requestPath, getClientIpAddress(request), clientKey);
            
            boolean allowed;
            
            if (isLoginRequest) {
                // Endpoint de login: aplica rate limit mais restritivo
                // Protege contra ataques de força bruta
                logger.debug("Aplicando rate limit específico para login. Limite: {} tentativas", loginCapacity);
                
                allowed = rateLimitService.tryConsume(
                    clientKey, 
                    loginCapacity, 
                    loginRefillTokens, 
                    loginRefillDuration
                );
                
                if (!allowed) {
                    logger.warn("Rate limit excedido para login. IP: {}, Endpoint: {}", 
                            getClientIpAddress(request), requestPath);
                    handleRateLimitExceeded(response, clientKey, loginCapacity, loginRefillTokens, loginRefillDuration);
                    return;
                }
                
                logger.debug("Rate limit OK para login. Tokens restantes no bucket");
            } else {
                // Outros endpoints: aplica rate limit padrão
                // Configurado via application.properties
                allowed = rateLimitService.tryConsume(clientKey);
                
                if (!allowed) {
                    logger.warn("Rate limit excedido. IP: {}, Endpoint: {}", 
                            getClientIpAddress(request), requestPath);
                    handleRateLimitExceeded(response, clientKey);
                    return;
                }
                
                logger.debug("Rate limit OK. Requisição permitida");
            }
            
            // Rate limit OK: continua o processamento da requisição
            filterChain.doFilter(request, response);
            
        } catch (RateLimitExceededException e) {
            // Trata exceção de rate limit excedido
            logger.warn("Exceção de rate limit capturada: {}", e.getMessage());
            handleRateLimitExceeded(response, e);
        }
    }

    /**
     * Gera uma chave única para identificar o bucket de rate limiting.
     * 
     * A chave combina o IP do cliente com o endpoint acessado, permitindo que:
     * - Cada IP tenha seu próprio bucket (rate limit individual)
     * - Cada endpoint tenha seu próprio bucket (diferentes limites por rota)
     * 
     * Exemplo de chaves geradas:
     * - "192.168.1.1:/login" (bucket específico para login deste IP)
     * - "192.168.1.1:/api/usuarios" (bucket para outro endpoint do mesmo IP)
     * 
     * @param request Objeto HttpServletRequest contendo informações da requisição
     * @return Chave única no formato "IP:endpoint"
     */
    private String getClientKey(HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String path = request.getRequestURI();
        
        // Combina IP e path para ter rate limits independentes por endpoint
        String key = ipAddress + ":" + path;
        logger.trace("Chave gerada para rate limiting: {}", key);
        return key;
    }

    /**
     * Obtém o endereço IP real do cliente, considerando proxies e load balancers.
     * 
     * Em ambientes de produção com proxies reversos ou load balancers,
     * o IP do cliente pode estar em headers HTTP específicos. Este método
     * verifica na seguinte ordem de prioridade:
     * 
     * 1. X-Forwarded-For: Header padrão usado por proxies
     *    - Pode conter múltiplos IPs separados por vírgula
     *    - O primeiro IP é sempre o IP original do cliente
     * 
     * 2. X-Real-IP: Header usado por alguns load balancers (ex: Nginx)
     *    - Contém diretamente o IP do cliente
     * 
     * 3. RemoteAddr: IP direto da conexão (fallback quando não há proxy)
     * 
     * @param request Objeto HttpServletRequest contendo informações da requisição
     * @return Endereço IP real do cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // X-Forwarded-For pode conter múltiplos IPs: "client, proxy1, proxy2"
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Pega o primeiro IP da lista (IP original do cliente)
            String ip = xForwardedFor.split(",")[0].trim();
            logger.trace("IP obtido do header X-Forwarded-For: {}", ip);
            return ip;
        }
        
        // X-Real-IP é usado por alguns load balancers
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            logger.trace("IP obtido do header X-Real-IP: {}", xRealIp);
            return xRealIp;
        }
        
        // Fallback: IP direto da conexão
        String remoteAddr = request.getRemoteAddr();
        logger.trace("IP obtido do RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Trata quando o rate limit é excedido usando a configuração padrão.
     * 
     * Este método é chamado quando uma requisição excede o rate limit padrão
     * (não é uma requisição de login). Retorna uma resposta HTTP 429 (Too Many Requests)
     * com informações sobre quando o cliente pode tentar novamente.
     * 
     * Headers HTTP incluídos:
     * - Retry-After: Tempo em segundos até o próximo token estar disponível
     * - X-RateLimit-Limit: Limite máximo de requisições
     * - X-RateLimit-Remaining: Tokens restantes no bucket
     * 
     * @param response Objeto HttpServletResponse para enviar a resposta
     * @param clientKey Chave única do cliente (IP:endpoint)
     * @throws IOException se ocorrer erro ao escrever a resposta
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientKey) throws IOException {
        logger.debug("Tratando rate limit excedido (padrão) para chave: {}", clientKey);
        
        // Obtém informações do bucket para calcular tempo de espera
        var bucketInfo = rateLimitService.getBucketInfo(clientKey);
        
        // Define status HTTP 429 (Too Many Requests)
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        
        // Headers informativos para o cliente
        response.setHeader("Retry-After", String.valueOf(bucketInfo.getWaitTimeSeconds()));
        response.setHeader("X-RateLimit-Limit", "Rate limit excedido");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucketInfo.getAvailableTokens()));
        
        // Monta resposta JSON com informações sobre o bloqueio
        String jsonResponse = String.format(
            "{\"error\":\"Rate limit excedido\",\"message\":\"Muitas requisições. Tente novamente em %d segundos.\",\"retryAfter\":%d}",
            bucketInfo.getWaitTimeSeconds(),
            bucketInfo.getWaitTimeSeconds()
        );
        
        response.getWriter().write(jsonResponse);
        logger.debug("Resposta de rate limit enviada. Tempo de espera: {} segundos", 
                bucketInfo.getWaitTimeSeconds());
    }

    /**
     * Trata quando o rate limit é excedido usando configuração customizada (para login).
     * 
     * Este método é chamado quando uma requisição de login excede o rate limit específico.
     * Retorna uma resposta HTTP 429 com mensagem mais detalhada sobre o bloqueio de login.
     * 
     * A mensagem inclui:
     * - Número máximo de tentativas permitidas
     * - Tempo de espera formatado em minutos/segundos
     * - Informações de retry
     * 
     * @param response Objeto HttpServletResponse para enviar a resposta
     * @param clientKey Chave única do cliente (IP:/login)
     * @param capacity Capacidade máxima do bucket (número de tentativas permitidas)
     * @param refillTokens Quantidade de tokens a serem repostos
     * @param refillDuration Duração em segundos para repor os tokens
     * @throws IOException se ocorrer erro ao escrever a resposta
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientKey, 
                                         long capacity, long refillTokens, long refillDuration) throws IOException {
        logger.warn("Rate limit de login excedido. Chave: {}, Limite: {} tentativas", clientKey, capacity);
        
        // Obtém informações do bucket de login
        var bucketInfo = rateLimitService.getBucketInfo(clientKey, capacity, refillTokens, refillDuration);
        
        // Define status HTTP 429 (Too Many Requests)
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        
        // Headers informativos
        response.setHeader("Retry-After", String.valueOf(bucketInfo.getWaitTimeSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucketInfo.getAvailableTokens()));
        
        // Formata o tempo de espera de forma mais legível (minutos ou segundos)
        long waitMinutes = bucketInfo.getWaitTimeSeconds() / 60;
        String timeMessage = waitMinutes > 0 
            ? String.format("%d minuto%s", waitMinutes, waitMinutes > 1 ? "s" : "")
            : String.format("%d segundo%s", bucketInfo.getWaitTimeSeconds(), bucketInfo.getWaitTimeSeconds() != 1 ? "s" : "");
        
        // Monta resposta JSON específica para login bloqueado
        String jsonResponse = String.format(
            "{\"error\":\"Rate limit excedido\",\"message\":\"Muitas tentativas de login. Você excedeu o limite de %d tentativas. Tente novamente em %s.\",\"retryAfter\":%d,\"maxAttempts\":%d}",
            capacity,
            timeMessage,
            bucketInfo.getWaitTimeSeconds(),
            capacity
        );
        
        response.getWriter().write(jsonResponse);
        logger.debug("Resposta de rate limit de login enviada. Tempo de espera: {} ({})", 
                bucketInfo.getWaitTimeSeconds(), timeMessage);
    }

    /**
     * Trata exceção de rate limit excedido lançada pelo RateLimitService.
     * 
     * Este método é chamado quando uma exceção RateLimitExceededException é lançada,
     * convertendo-a em uma resposta HTTP apropriada.
     * 
     * @param response Objeto HttpServletResponse para enviar a resposta
     * @param e Exceção contendo informações sobre o rate limit excedido
     * @throws IOException se ocorrer erro ao escrever a resposta
     */
    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitExceededException e) throws IOException {
        logger.warn("Exceção de rate limit capturada: {}", e.getMessage());
        
        // Define status HTTP 429 (Too Many Requests)
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        
        // Monta resposta JSON com a mensagem da exceção
        String jsonResponse = String.format(
            "{\"error\":\"Rate limit excedido\",\"message\":\"%s\",\"retryAfter\":%d}",
            e.getMessage(),
            e.getRetryAfterSeconds()
        );
        
        response.getWriter().write(jsonResponse);
        logger.debug("Resposta de rate limit (exceção) enviada. Tempo de espera: {} segundos", 
                e.getRetryAfterSeconds());
    }
}

