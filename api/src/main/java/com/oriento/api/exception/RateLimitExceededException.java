package com.oriento.api.exception;

/**
 * Exceção lançada quando o rate limit é excedido.
 * 
 * Esta exceção é lançada pelo RateLimitService quando uma requisição tenta
 * consumir um token de um bucket que não possui tokens disponíveis.
 * 
 * A exceção contém informações úteis para o cliente:
 * - Mensagem descritiva do erro
 * - Tempo de espera em segundos até o próximo token estar disponível
 * 
 * Esta informação é usada para:
 * - Retornar HTTP 429 (Too Many Requests) ao cliente
 * - Incluir header Retry-After na resposta HTTP
 * - Fornecer feedback claro sobre quando o cliente pode tentar novamente
 */
public class RateLimitExceededException extends RuntimeException {
    
    /**
     * Tempo em segundos que o cliente deve esperar antes de tentar novamente.
     * 
     * Este valor é calculado pelo Bucket4j baseado na estratégia de reposição
     * de tokens configurada no Bandwidth.
     */
    private final long retryAfterSeconds;
    
    /**
     * Construtor da exceção de rate limit excedido.
     * 
     * @param message Mensagem descritiva do erro (ex: "Rate limit excedido. Tente novamente em X segundos.")
     * @param retryAfterSeconds Tempo em segundos até o próximo token estar disponível
     */
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    /**
     * Retorna o tempo de espera em segundos até o próximo token estar disponível.
     * 
     * Este valor pode ser usado para:
     * - Definir o header HTTP Retry-After
     * - Exibir mensagem ao usuário sobre quando pode tentar novamente
     * - Implementar backoff automático no cliente
     * 
     * @return Tempo de espera em segundos
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

