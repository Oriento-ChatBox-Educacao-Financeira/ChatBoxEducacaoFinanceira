package com.oriento.api.services.ratelimit;

import com.oriento.api.config.ratelimit.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final RateLimitConfig rateLimitConfig;

    @Autowired
    public RateLimitService(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
        logger.info("RateLimitService inicializado com sucesso");
    }

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            logger.debug("Criando novo bucket com configuração padrão para chave: {}", key);
            Bandwidth bandwidth = rateLimitConfig.defaultBandwidth();
            Bucket bucket = Bucket4j.builder()
                    .addLimit(bandwidth)
                    .build();
            logger.debug("Bucket criado com sucesso para chave: {}", key);
            return bucket;
        });
    }

    public Bucket resolveBucket(String key, long capacity, long refillTokens, long refillDurationSeconds) {
        return buckets.computeIfAbsent(key, k -> {
            logger.debug("Criando novo bucket com configuração customizada para chave: {}. Capacidade: {}, Refill: {} tokens a cada {} segundos", 
                    key, capacity, refillTokens, refillDurationSeconds);
            Bandwidth bandwidth = rateLimitConfig.createBandwidth(capacity, refillTokens, refillDurationSeconds);
            Bucket bucket = Bucket4j.builder()
                    .addLimit(bandwidth)
                    .build();
            logger.debug("Bucket customizado criado com sucesso para chave: {}", key);
            return bucket;
        });
    }

    public boolean tryConsume(String key) {
        Bucket bucket = resolveBucket(key);
        boolean consumed = bucket.tryConsume(1);
        
        if (consumed) {
            logger.trace("Token consumido com sucesso do bucket: {}", key);
        } else {
            logger.debug("Falha ao consumir token do bucket: {} (sem tokens disponíveis)", key);
        }
        
        return consumed;
    }

    public boolean tryConsume(String key, long capacity, long refillTokens, long refillDurationSeconds) {
        Bucket bucket = resolveBucket(key, capacity, refillTokens, refillDurationSeconds);
        boolean consumed = bucket.tryConsume(1);
        
        if (consumed) {
            logger.trace("Token consumido com sucesso do bucket customizado: {}", key);
        } else {
            logger.debug("Falha ao consumir token do bucket customizado: {} (sem tokens disponíveis)", key);
        }
        
        return consumed;
    }

    public boolean consume(String key) {
        Bucket bucket = resolveBucket(key);
        
        if (bucket.tryConsume(1)) {
            logger.trace("Token consumido do bucket: {}", key);
            return true;
        }
        
        // Calcula o tempo de espera até o próximo token estar disponível
        long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        long waitTimeSeconds = waitTimeNanos / 1_000_000_000;
        
        logger.warn("Tentativa de consumir token falhou. Bucket: {}, Tempo de espera: {} segundos", 
                key, waitTimeSeconds);
        
        throw new com.oriento.api.exception.RateLimitExceededException(
                "Rate limit excedido. Tente novamente em " + waitTimeSeconds + " segundos.",
                waitTimeSeconds
        );
    }

    public BucketInfo getBucketInfo(String key) {
        Bucket bucket = resolveBucket(key);
        long availableTokens = bucket.getAvailableTokens();
        long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        long waitTimeSeconds = waitTimeNanos / 1_000_000_000;
        
        logger.debug("Informações do bucket {}: {} tokens disponíveis, {} segundos de espera", 
                key, availableTokens, waitTimeSeconds);
        
        return new BucketInfo(availableTokens, waitTimeSeconds);
    }

    public BucketInfo getBucketInfo(String key, long capacity, long refillTokens, long refillDurationSeconds) {
        Bucket bucket = resolveBucket(key, capacity, refillTokens, refillDurationSeconds);
        long availableTokens = bucket.getAvailableTokens();
        long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        long waitTimeSeconds = waitTimeNanos / 1_000_000_000;
        
        logger.debug("Informações do bucket customizado {}: {} tokens disponíveis, {} segundos de espera", 
                key, availableTokens, waitTimeSeconds);
        
        return new BucketInfo(availableTokens, waitTimeSeconds);
    }

    public void removeBucket(String key) {
        Bucket removed = buckets.remove(key);
        if (removed != null) {
            logger.info("Bucket removido do cache: {}", key);
        } else {
            logger.debug("Tentativa de remover bucket inexistente: {}", key);
        }
    }

    public void clearAllBuckets() {
        int size = buckets.size();
        buckets.clear();
        logger.warn("Todos os buckets foram limpos do cache. Total removido: {}", size);
    }

    public static class BucketInfo {
        private final long availableTokens;
        private final long waitTimeSeconds;

        public BucketInfo(long availableTokens, long waitTimeSeconds) {
            this.availableTokens = availableTokens;
            this.waitTimeSeconds = waitTimeSeconds;
        }

        public long getAvailableTokens() {
            return availableTokens;
        }

        public long getWaitTimeSeconds() {
            return waitTimeSeconds;
        }
    }
}

