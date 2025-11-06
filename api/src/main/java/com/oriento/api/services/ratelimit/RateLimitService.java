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

/**
 * Serviço responsável por gerenciar buckets de rate limiting usando Bucket4j.
 * 
 * Este serviço implementa o algoritmo Token Bucket para controlar a taxa de requisições.
 * Cada cliente (identificado por uma chave única) possui seu próprio bucket que:
 * - Contém um número limitado de tokens (capacidade)
 * - Repõe tokens periodicamente (refill)
 * - Bloqueia requisições quando não há tokens disponíveis
 * 
 * Funcionalidades:
 * - Criação e gerenciamento de buckets por chave (IP:endpoint)
 * - Suporte a configurações padrão e customizadas
 * - Thread-safe usando ConcurrentHashMap
 * - Informações sobre estado dos buckets (tokens disponíveis, tempo de espera)
 * 
 * O serviço mantém buckets em memória. Para ambientes distribuídos, considere
 * usar uma implementação distribuída do Bucket4j (Redis, Hazelcast, etc).
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * Mapa thread-safe que armazena buckets de rate limiting.
     * Chave: identificador único do cliente (ex: "192.168.1.1:/login")
     * Valor: Bucket do Bucket4j que gerencia os tokens
     * 
     * Usa ConcurrentHashMap para garantir thread-safety em ambientes multi-threaded.
     * Os buckets são criados sob demanda (lazy initialization) quando necessário.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    /**
     * Configuração de rate limiting que fornece os parâmetros padrão
     * para criação de buckets (capacidade, refill tokens, duração).
     */
    private final RateLimitConfig rateLimitConfig;

    /**
     * Construtor do serviço de rate limiting.
     * 
     * @param rateLimitConfig Configuração que fornece parâmetros padrão para buckets
     */
    @Autowired
    public RateLimitService(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
        logger.info("RateLimitService inicializado com sucesso");
    }

    /**
     * Resolve (obtém ou cria) um bucket usando a configuração padrão.
     * 
     * Se o bucket já existe para a chave fornecida, retorna o bucket existente.
     * Caso contrário, cria um novo bucket com as configurações padrão definidas
     * no RateLimitConfig (via application.properties).
     * 
     * O método computeIfAbsent garante thread-safety, criando o bucket apenas
     * uma vez mesmo em ambientes concorrentes.
     * 
     * @param key Chave única que identifica o bucket (ex: "192.168.1.1:/login")
     * @return Bucket configurado com os parâmetros padrão
     */
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

    /**
     * Resolve (obtém ou cria) um bucket com configurações customizadas.
     * 
     * Útil para endpoints que precisam de limites diferentes do padrão,
     * como o endpoint de login que tem limite mais restritivo (5 tentativas).
     * 
     * @param key Chave única que identifica o bucket
     * @param capacity Capacidade máxima do bucket (número máximo de tokens)
     * @param refillTokens Quantidade de tokens a serem repostos
     * @param refillDurationSeconds Duração em segundos para repor os tokens
     * @return Bucket configurado com os parâmetros customizados
     */
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

    /**
     * Tenta consumir um token do bucket usando configuração padrão.
     * 
     * Este método não lança exceção se não houver tokens disponíveis,
     * apenas retorna false. Útil para verificações não-bloqueantes.
     * 
     * @param key Chave que identifica o bucket
     * @return true se o token foi consumido com sucesso, false caso contrário
     */
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

    /**
     * Tenta consumir um token do bucket usando configuração customizada.
     * 
     * Similar ao método anterior, mas usa configurações específicas para o bucket.
     * Útil para endpoints com limites diferentes (ex: login com 5 tentativas).
     * 
     * @param key Chave que identifica o bucket
     * @param capacity Capacidade máxima do bucket
     * @param refillTokens Tokens a serem repostos
     * @param refillDurationSeconds Duração em segundos para repor os tokens
     * @return true se o token foi consumido com sucesso, false caso contrário
     */
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

    /**
     * Consome um token do bucket, lançando exceção se não houver tokens disponíveis.
     * 
     * Diferente do tryConsume, este método lança uma exceção RateLimitExceededException
     * quando não há tokens disponíveis, incluindo informações sobre o tempo de espera.
     * 
     * Útil quando você quer que a exceção seja propagada para tratamento em camadas superiores.
     * 
     * @param key Chave que identifica o bucket
     * @return true se o token foi consumido com sucesso
     * @throws com.oriento.api.exception.RateLimitExceededException se não houver tokens disponíveis
     */
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

    /**
     * Obtém informações sobre o estado atual de um bucket (configuração padrão).
     * 
     * Retorna informações úteis para debug, monitoramento ou para incluir
     * em respostas HTTP (headers X-RateLimit-*).
     * 
     * @param key Chave que identifica o bucket
     * @return BucketInfo contendo tokens disponíveis e tempo de espera
     */
    public BucketInfo getBucketInfo(String key) {
        Bucket bucket = resolveBucket(key);
        long availableTokens = bucket.getAvailableTokens();
        long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        long waitTimeSeconds = waitTimeNanos / 1_000_000_000;
        
        logger.debug("Informações do bucket {}: {} tokens disponíveis, {} segundos de espera", 
                key, availableTokens, waitTimeSeconds);
        
        return new BucketInfo(availableTokens, waitTimeSeconds);
    }

    /**
     * Obtém informações sobre o estado atual de um bucket (configuração customizada).
     * 
     * Similar ao método anterior, mas para buckets com configurações específicas.
     * 
     * @param key Chave que identifica o bucket
     * @param capacity Capacidade máxima do bucket
     * @param refillTokens Tokens a serem repostos
     * @param refillDurationSeconds Duração em segundos para repor os tokens
     * @return BucketInfo contendo tokens disponíveis e tempo de espera
     */
    public BucketInfo getBucketInfo(String key, long capacity, long refillTokens, long refillDurationSeconds) {
        Bucket bucket = resolveBucket(key, capacity, refillTokens, refillDurationSeconds);
        long availableTokens = bucket.getAvailableTokens();
        long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        long waitTimeSeconds = waitTimeNanos / 1_000_000_000;
        
        logger.debug("Informações do bucket customizado {}: {} tokens disponíveis, {} segundos de espera", 
                key, availableTokens, waitTimeSeconds);
        
        return new BucketInfo(availableTokens, waitTimeSeconds);
    }

    /**
     * Remove um bucket específico do cache.
     * 
     * Útil para limpeza manual ou reset de rate limits para um cliente específico.
     * Após a remoção, o próximo acesso criará um novo bucket.
     * 
     * @param key Chave do bucket a ser removido
     */
    public void removeBucket(String key) {
        Bucket removed = buckets.remove(key);
        if (removed != null) {
            logger.info("Bucket removido do cache: {}", key);
        } else {
            logger.debug("Tentativa de remover bucket inexistente: {}", key);
        }
    }

    /**
     * Limpa todos os buckets do cache.
     * 
     * ATENÇÃO: Este método remove TODOS os buckets, resetando todos os rate limits.
     * Use com cuidado, preferencialmente apenas em situações de emergência ou
     * durante manutenção.
     */
    public void clearAllBuckets() {
        int size = buckets.size();
        buckets.clear();
        logger.warn("Todos os buckets foram limpos do cache. Total removido: {}", size);
    }

    /**
     * Classe que encapsula informações sobre o estado atual de um bucket.
     * 
     * Usada para retornar informações úteis sobre tokens disponíveis e
     * tempo de espera, que podem ser incluídas em respostas HTTP ou logs.
     */
    public static class BucketInfo {
        /** Número de tokens disponíveis no bucket no momento */
        private final long availableTokens;
        
        /** Tempo em segundos até o próximo token estar disponível (se não houver tokens) */
        private final long waitTimeSeconds;

        /**
         * Construtor da classe BucketInfo.
         * 
         * @param availableTokens Número de tokens disponíveis
         * @param waitTimeSeconds Tempo de espera em segundos
         */
        public BucketInfo(long availableTokens, long waitTimeSeconds) {
            this.availableTokens = availableTokens;
            this.waitTimeSeconds = waitTimeSeconds;
        }

        /**
         * @return Número de tokens disponíveis no bucket
         */
        public long getAvailableTokens() {
            return availableTokens;
        }

        /**
         * @return Tempo em segundos até o próximo token estar disponível
         */
        public long getWaitTimeSeconds() {
            return waitTimeSeconds;
        }
    }
}

