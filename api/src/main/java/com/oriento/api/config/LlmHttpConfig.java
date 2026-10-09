package com.oriento.api.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

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

    /**
     * WebClient dedicado para chamadas streaming ao Ollama.
     */
    @Bean
    public WebClient llmWebClient(
            @Value("${llm.api.base-url}") String baseUrl,
            @Value("${llm.api.connect-timeout}") Duration connectTimeout,
            @Value("${llm.api.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(readTimeout);

        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return WebClient.builder()
                .baseUrl(normalizedBase)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();
    }
}
