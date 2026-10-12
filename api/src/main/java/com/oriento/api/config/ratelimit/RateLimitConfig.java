package com.oriento.api.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

    @Value("${ratelimit.default.capacity:100}")
    private long defaultCapacity;

    @Value("${ratelimit.default.refill.tokens:100}")
    private long defaultRefillTokens;

    @Value("${ratelimit.default.refill.duration:60}")
    private long defaultRefillDuration;

    @Bean
    public Bandwidth defaultBandwidth() {
        logger.info("Criando Bandwidth padrão. Capacidade: {}, Refill: {} tokens a cada {} segundos", 
                defaultCapacity, defaultRefillTokens, defaultRefillDuration);
        
        return Bandwidth.classic(
                defaultCapacity,
                Refill.intervally(defaultRefillTokens, Duration.ofSeconds(defaultRefillDuration))
        );
    }

    public Bandwidth createBandwidth(long capacity, long refillTokens, long refillDurationSeconds) {
        logger.debug("Criando Bandwidth customizado. Capacidade: {}, Refill: {} tokens a cada {} segundos", 
                capacity, refillTokens, refillDurationSeconds);
        
        return Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofSeconds(refillDurationSeconds))
        );
    }

    public long getDefaultCapacity() {
        return defaultCapacity;
    }
    public long getDefaultRefillTokens() {
        return defaultRefillTokens;
    }
    public long getDefaultRefillDuration() {
        return defaultRefillDuration;
    }
}

