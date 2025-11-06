package com.oriento.api.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração de Rate Limiting usando Bucket4j.
 * 
 * Esta classe fornece configurações para criação de Bandwidths (limites de taxa)
 * usados pelos buckets de rate limiting. Os valores podem ser configurados via
 * application.properties ou usam valores padrão.
 * 
 * Bandwidth define:
 * - Capacidade: número máximo de tokens no bucket
 * - Refill: quantidade de tokens repostos e frequência de reposição
 * 
 * Configurações disponíveis em application.properties:
 * - ratelimit.default.capacity: capacidade padrão (padrão: 100)
 * - ratelimit.default.refill.tokens: tokens a repor (padrão: 100)
 * - ratelimit.default.refill.duration: duração em segundos para repor (padrão: 60)
 */
@Configuration
public class RateLimitConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

    /**
     * Capacidade padrão do bucket (número máximo de tokens).
     * 
     * Valor padrão: 100 tokens
     * Configurável via: ratelimit.default.capacity
     */
    @Value("${ratelimit.default.capacity:100}")
    private long defaultCapacity;

    /**
     * Quantidade de tokens a serem repostos no bucket.
     * 
     * Valor padrão: 100 tokens
     * Configurável via: ratelimit.default.refill.tokens
     */
    @Value("${ratelimit.default.refill.tokens:100}")
    private long defaultRefillTokens;

    /**
     * Duração em segundos para repor os tokens no bucket.
     * 
     * Valor padrão: 60 segundos (1 minuto)
     * Configurável via: ratelimit.default.refill.duration
     * 
     * Exemplo: se for 60, o bucket repõe os tokens a cada 60 segundos.
     */
    @Value("${ratelimit.default.refill.duration:60}")
    private long defaultRefillDuration;

    /**
     * Cria um Bandwidth padrão configurado com os valores do application.properties.
     * 
     * Este Bandwidth é usado como padrão para todos os buckets que não especificam
     * configurações customizadas. Define a capacidade e a estratégia de reposição
     * de tokens usando o algoritmo "intervally" (reposição periódica).
     * 
     * @return Bandwidth configurado com valores padrão
     */
    @Bean
    public Bandwidth defaultBandwidth() {
        logger.info("Criando Bandwidth padrão. Capacidade: {}, Refill: {} tokens a cada {} segundos", 
                defaultCapacity, defaultRefillTokens, defaultRefillDuration);
        
        return Bandwidth.classic(
                defaultCapacity,
                Refill.intervally(defaultRefillTokens, Duration.ofSeconds(defaultRefillDuration))
        );
    }

    /**
     * Cria um Bandwidth customizado com parâmetros específicos.
     * 
     * Útil para criar buckets com limites diferentes do padrão, como o endpoint
     * de login que precisa de limite mais restritivo (5 tentativas por 15 minutos).
     * 
     * @param capacity Capacidade máxima do bucket (número máximo de tokens)
     * @param refillTokens Quantidade de tokens a serem repostos
     * @param refillDurationSeconds Duração em segundos para repor os tokens
     * @return Bandwidth configurado com os parâmetros fornecidos
     */
    public Bandwidth createBandwidth(long capacity, long refillTokens, long refillDurationSeconds) {
        logger.debug("Criando Bandwidth customizado. Capacidade: {}, Refill: {} tokens a cada {} segundos", 
                capacity, refillTokens, refillDurationSeconds);
        
        return Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofSeconds(refillDurationSeconds))
        );
    }

    /**
     * @return Capacidade padrão configurada
     */
    public long getDefaultCapacity() {
        return defaultCapacity;
    }

    /**
     * @return Quantidade de tokens a serem repostos (padrão)
     */
    public long getDefaultRefillTokens() {
        return defaultRefillTokens;
    }

    /**
     * @return Duração em segundos para repor os tokens (padrão)
     */
    public long getDefaultRefillDuration() {
        return defaultRefillDuration;
    }
}

