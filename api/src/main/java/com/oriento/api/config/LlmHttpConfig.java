package com.oriento.api.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LlmHttpConfig {

    @Bean
    public RestTemplate llmRestTemplate(
            RestTemplateBuilder builder,
            @Value("${llm.api.connect-timeout}") Duration connectTimeout,
            @Value("${llm.api.read-timeout}") Duration readTimeout) {
        return builder
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }
}
