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

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private final RateLimitService rateLimitService;

    @Value("${ratelimit.login.capacity:5}")
    private long loginCapacity;

    @Value("${ratelimit.login.refill.tokens:5}")
    private long loginRefillTokens;

    @Value("${ratelimit.login.refill.duration:900}")
    private long loginRefillDuration;

    @Autowired
    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        logger.info("RateLimitFilter inicializado. Login limit: {} tentativas a cada {} segundos", 
                loginCapacity, loginRefillDuration);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String clientKey = getClientKey(request);
            String requestPath = request.getRequestURI();
            boolean isLoginRequest = "/login".equals(requestPath);
            
            logger.debug("Processando rate limit. Endpoint: {}, IP: {}, Chave: {}", 
                    requestPath, getClientIpAddress(request), clientKey);
            
            boolean allowed;
            
            if (isLoginRequest) {
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
                allowed = rateLimitService.tryConsume(clientKey);
                
                if (!allowed) {
                    logger.warn("Rate limit excedido. IP: {}, Endpoint: {}", 
                            getClientIpAddress(request), requestPath);
                    handleRateLimitExceeded(response, clientKey);
                    return;
                }
                
                logger.debug("Rate limit OK. Requisição permitida");
            }

            filterChain.doFilter(request, response);
            
        } catch (RateLimitExceededException e) {
            logger.warn("Exceção de rate limit capturada: {}", e.getMessage());
            handleRateLimitExceeded(response, e);
        }
    }

    private String getClientKey(HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String path = request.getRequestURI();
        String key = ipAddress + ":" + path;
        logger.trace("Chave gerada para rate limiting: {}", key);
        return key;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String ip = xForwardedFor.split(",")[0].trim();
            logger.trace("IP obtido do header X-Forwarded-For: {}", ip);
            return ip;
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            logger.trace("IP obtido do header X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddr();
        logger.trace("IP obtido do RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String clientKey) throws IOException {
        logger.debug("Tratando rate limit excedido (padrão) para chave: {}", clientKey);

        var bucketInfo = rateLimitService.getBucketInfo(clientKey);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        response.setHeader("Retry-After", String.valueOf(bucketInfo.getWaitTimeSeconds()));
        response.setHeader("X-RateLimit-Limit", "Rate limit excedido");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucketInfo.getAvailableTokens()));

        String jsonResponse = String.format(
            "{\"error\":\"Rate limit excedido\",\"message\":\"Muitas requisições. Tente novamente em %d segundos.\",\"retryAfter\":%d}",
            bucketInfo.getWaitTimeSeconds(),
            bucketInfo.getWaitTimeSeconds()
        );
        
        response.getWriter().write(jsonResponse);
        logger.debug("Resposta de rate limit enviada. Tempo de espera: {} segundos", 
                bucketInfo.getWaitTimeSeconds());
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String clientKey, 
                                         long capacity, long refillTokens, long refillDuration) throws IOException {
        logger.warn("Rate limit de login excedido. Chave: {}, Limite: {} tentativas", clientKey, capacity);

        var bucketInfo = rateLimitService.getBucketInfo(clientKey, capacity, refillTokens, refillDuration);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        response.setHeader("Retry-After", String.valueOf(bucketInfo.getWaitTimeSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucketInfo.getAvailableTokens()));

        long waitMinutes = bucketInfo.getWaitTimeSeconds() / 60;
        String timeMessage = waitMinutes > 0 
            ? String.format("%d minuto%s", waitMinutes, waitMinutes > 1 ? "s" : "")
            : String.format("%d segundo%s", bucketInfo.getWaitTimeSeconds(), bucketInfo.getWaitTimeSeconds() != 1 ? "s" : "");

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

    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitExceededException e) throws IOException {
        logger.warn("Exceção de rate limit capturada: {}", e.getMessage());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));

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

