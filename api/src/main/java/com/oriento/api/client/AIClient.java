package com.oriento.api.client;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIClient {

    private static final Logger logger = LoggerFactory.getLogger(AIClient.class);

    @Value("${ai.api.key}")
    private String apiKey;

    @Bean
    public Client createGeminiClient() {
        logger.info("Inicializando cliente da AI...");

        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API key do AI não configurada");
            throw new IllegalStateException(
                "AI API key não configurada. Configure a propriedade 'AI.api.key' " +
                "ou a variável de ambiente 'AI_API_KEY'"
            );
        }
        logger.debug("API key do AI encontrada. Criando cliente...");
        
        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        logger.info("Cliente da AI criado com sucesso");
        return client;
    }

}

