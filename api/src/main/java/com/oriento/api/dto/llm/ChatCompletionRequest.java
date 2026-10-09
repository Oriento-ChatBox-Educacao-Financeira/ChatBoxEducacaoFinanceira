package com.oriento.api.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Payload do endpoint {@code /api/chat} do Ollama.
 *
 * <p>{@code stream} controla se a resposta vem em NDJSON. {@code options}
 * encapsula parâmetros de inferência (num_ctx, num_predict, temperature, ...)
 * e {@code keepAlive} mantém o modelo em VRAM entre requests evitando
 * cold-start.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        boolean stream,
        List<ChatMessage> messages,
        Map<String, Object> options,
        @JsonProperty("keep_alive") String keepAlive) {

    public ChatCompletionRequest(String model, boolean stream, List<ChatMessage> messages) {
        this(model, stream, messages, null, null);
    }
}
