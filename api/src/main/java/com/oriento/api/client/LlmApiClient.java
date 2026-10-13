package com.oriento.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriento.api.dto.llm.ChatCompletionRequest;
import com.oriento.api.dto.llm.ChatCompletionResponse;
import com.oriento.api.dto.llm.ChatMessage;
import com.oriento.api.exception.LlmApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Cliente para a API {@code /api/chat} do Ollama.
 *
 * <p>Suporta dois modos:</p>
 * <ul>
 *   <li>{@link #chat(List)} (síncrono) — usa {@link RestTemplate}, retorna a resposta
 *       completa de uma vez. Mantido para clientes que não consomem streaming.</li>
 *   <li>{@link #chatStream(List)} (streaming) — usa {@link WebClient} para consumir
 *       o NDJSON do Ollama linha-a-linha e emite só o delta de
 *       {@code message.content} via {@link Flux}.</li>
 * </ul>
 *
 * <p>Em ambos os modos enviamos {@code options} (num_ctx, num_predict, ...)
 * e {@code keep_alive} para evitar cold-start em chamadas subsequentes.</p>
 */
@Component
public class LlmApiClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmApiClient.class);

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String model;
    private final String keepAlive;
    private final Map<String, Object> options;

    public LlmApiClient(
            RestTemplate llmRestTemplate,
            WebClient llmWebClient,
            @Value("${llm.api.base-url}") String baseUrl,
            @Value("${llm.api.model}") String model,
            @Value("${llm.api.keep-alive:30m}") String keepAlive,
            @Value("${llm.api.options.num-ctx:4096}") int numCtx,
            @Value("${llm.api.options.num-predict:600}") int numPredict,
            @Value("${llm.api.options.temperature:0.4}") double temperature,
            @Value("${llm.api.options.top-p:0.9}") double topP,
            @Value("${llm.api.options.repeat-penalty:1.1}") double repeatPenalty) {
        this.restTemplate = llmRestTemplate;
        this.webClient = llmWebClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.keepAlive = keepAlive;

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("num_ctx", numCtx);
        opts.put("num_predict", numPredict);
        opts.put("temperature", temperature);
        opts.put("top_p", topP);
        opts.put("repeat_penalty", repeatPenalty);
        this.options = Map.copyOf(opts);

        logger.info("Cliente LLM configurado: baseUrl={}, model={}, keep_alive={}, options={}",
                this.baseUrl, this.model, this.keepAlive, this.options);
    }

    /**
     * Chat síncrono — espera a resposta completa do Ollama.
     */
    public String chat(List<ChatMessage> messages) {
        String url = baseUrl + "/api/chat";
        ChatCompletionRequest request = new ChatCompletionRequest(model, false, messages, options, keepAlive);

        logger.debug("Enviando requisição síncrona para LLM: {} mensagens", messages.size());

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

    /**
     * Chat em streaming — emite cada delta de {@code message.content} como
     * uma string assim que chega no NDJSON do Ollama. O {@link Flux} completa
     * quando o Ollama envia {@code done: true}.
     */
    public Flux<String> chatStream(List<ChatMessage> messages) {
        ChatCompletionRequest request = new ChatCompletionRequest(model, true, messages, options, keepAlive);

        logger.debug("Abrindo stream com LLM: {} mensagens", messages.size());

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(this::extrairConteudoDelta)
                .filter(s -> !s.isEmpty())
                .doOnError(err ->
                        logger.error("Falha no streaming com o servidor LLM: {}", err.getMessage()))
                .onErrorMap(err -> err instanceof LlmApiException
                        ? err
                        : new LlmApiException("Falha na comunicação com o servidor LLM: " + err.getMessage(), err));
    }

    private String extrairConteudoDelta(String linha) {
        if (linha == null || linha.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(linha);
            if (node.has("error")) {
                String msg = node.get("error").asText("erro desconhecido");
                throw new LlmApiException("Erro do servidor LLM: " + msg);
            }
            JsonNode message = node.get("message");
            if (message == null || message.get("content") == null) {
                return "";
            }
            return message.get("content").asText("");
        } catch (LlmApiException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Não foi possível parsear linha do stream LLM: {}", linha);
            return "";
        }
    }
}
