package com.oriento.api.client;

import com.oriento.api.dto.llm.ChatCompletionRequest;
import com.oriento.api.dto.llm.ChatCompletionResponse;
import com.oriento.api.dto.llm.ChatMessage;
import com.oriento.api.exception.LlmApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class LlmApiClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmApiClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String model;

    public LlmApiClient(
            RestTemplate llmRestTemplate,
            @Value("${llm.api.base-url}") String baseUrl,
            @Value("${llm.api.model}") String model) {
        this.restTemplate = llmRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        logger.info("Cliente LLM configurado: baseUrl={}, model={}", this.baseUrl, this.model);
    }

    public String chat(List<ChatMessage> messages) {
        String url = baseUrl + "/api/chat";
        ChatCompletionRequest request = new ChatCompletionRequest(model, false, messages);

        logger.debug("Enviando requisição para LLM: {} mensagens", messages.size());

        try {
            ResponseEntity<ChatCompletionResponse> response =
                    restTemplate.postForEntity(url, request, ChatCompletionResponse.class);

            ChatCompletionResponse body = response.getBody();
            if (body == null || body.message() == null || body.message().content() == null) {
                throw new LlmApiException("Resposta vazia do servidor LLM");
            }

            return body.message().content();
        } catch (RestClientException ex) {
            logger.error("Falha na comunicação com o servidor LLM em {}", url, ex);
            throw new LlmApiException("Falha na comunicação com o servidor LLM: " + ex.getMessage(), ex);
        }
    }
}
