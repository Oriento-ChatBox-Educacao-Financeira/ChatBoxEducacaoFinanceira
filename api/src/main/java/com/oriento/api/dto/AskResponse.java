package com.oriento.api.dto;

/**
 * Representa a resposta do endpoint de interação com o Gemini,
 * contendo a mensagem retornada pelo modelo e o identificador
 * da conversa para manutenção de contexto.
 */
public record AskResponse(String conversationId, String response) {
}

