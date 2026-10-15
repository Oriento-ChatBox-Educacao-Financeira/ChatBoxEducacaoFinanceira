package com.oriento.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(OrientoAiProperties.class)
public class OrientoHttpConfig {
    @Bean
    public RestTemplate orientoRestTemplate(RestTemplateBuilder builder, OrientoAiProperties properties) {
        return builder.connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout()).build();
    }
}
