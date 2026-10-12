package com.oriento.api.dto.llm;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        boolean stream,
        List<ChatMessage> messages) {
}
