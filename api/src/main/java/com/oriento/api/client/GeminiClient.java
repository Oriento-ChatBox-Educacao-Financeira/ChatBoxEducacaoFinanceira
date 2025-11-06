package com.oriento.api.client;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do cliente para a API do Google Gemini.
 * 
 * Esta classe configura e cria um cliente para interagir com a API do Google Gemini,
 * que é usada para processamento de linguagem natural e geração de conteúdo.
 * 
 * A API key pode ser configurada via:
 * - application.properties: gemini.api.key
 * - Variável de ambiente: GEMINI_API_KEY
 * 
 * O cliente é criado como um Bean Spring, permitindo injeção de dependência
 * em outros componentes da aplicação.
 */
@Configuration
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    /**
     * API key do Google Gemini.
     * 
     * Carregada de:
     * 1. application.properties (propriedade: gemini.api.key)
     * 2. Variável de ambiente (GEMINI_API_KEY)
     * 
     * IMPORTANTE: A API key deve ser mantida em segredo e não deve ser
     * commitada em repositórios públicos. Use application.secret.properties
     * ou variáveis de ambiente em produção.
     */
    @Value("${gemini.api.key}")
    private String apiKey;

    /**
     * Cria e configura o cliente da API do Google Gemini.
     * 
     * Este método:
     * 1. Valida se a API key foi configurada
     * 2. Cria o cliente usando o builder do Google Genai
     * 3. Retorna o cliente configurado como Bean Spring
     * 
     * O cliente pode ser injetado em serviços que precisam interagir
     * com a API do Gemini (ex: GeminiService).
     * 
     * @return Client configurado e pronto para uso
     * @throws IllegalStateException se a API key não estiver configurada
     */
    @Bean
    public Client createGeminiClient() {
        logger.info("Inicializando cliente do Google Gemini...");
        
        // Valida se a API key foi configurada
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API key do Gemini não configurada");
            throw new IllegalStateException(
                "Gemini API key não configurada. Configure a propriedade 'gemini.api.key' " +
                "ou a variável de ambiente 'GEMINI_API_KEY'"
            );
        }
        
        logger.debug("API key do Gemini encontrada. Criando cliente...");
        
        // Cria o cliente usando o builder
        Client client = Client.builder()
                .apiKey(apiKey)
                .build();
        
        logger.info("Cliente do Google Gemini criado com sucesso");
        return client;
    }

}
