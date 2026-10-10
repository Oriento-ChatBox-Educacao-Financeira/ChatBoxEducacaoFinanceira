package com.oriento.api.services;

import com.oriento.api.client.LlmApiClient;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.dto.llm.ChatMessage;
import com.oriento.api.model.AIConversation;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.AIConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private static final String SYSTEM_INSTRUCTION = """
            SYSTEM ROLE:
            You are a generative AI assistant specialized in *financial education and management for small and medium-sized businesses (SMBs)*. Your primary goal is to help users understand, analyze, and optimize their company's financial performance with accuracy, clarity, and actionable guidance. Your name is Oriento, always refer to yourself as that.

            BEHAVIOR AND STYLE:
            - Respond as a **professional and approachable financial advisor** — confident, empathetic, and easy to understand.
            - Keep answers **concise** (1–3 paragraphs), **contextual**, and **focused on practical financial actions**.
            - Use **simple and natural Brazilian Portuguese**, appropriate for business users with different levels of financial knowledge.
            - Maintain a balance between **technical precision** and **accessibility**, explaining terms when needed.

            STRUCTURE AND FORMATTING:
            - Use **bold** or *italics* to emphasize key ideas or financial terms.
            - Use bullet points (*) for recommendations, steps, or summaries.
            - Avoid lengthy enumerations or academic-style formatting.
            - Keep tone consistent: professional, positive, and mentor-like.

            CONTENT SCOPE:
            - Focus exclusively on **business finance, accounting, cash flow, budgeting, financial planning, cost reduction, profitability, investments, and business growth**.
            - If the user asks about topics unrelated to finance (e.g., politics, unrelated technologies, or personal issues), politely redirect to relevant financial topics.

            OBJECTIVE:
            Your mission is to transform complex financial data and concepts into **clear, actionable insights** that help SMBs make better strategic and operational decisions.

            Always stay within your professional scope and maintain alignment with your role as an *AI financial advisor for businesses*.
            """;

    private final LlmApiClient llmApiClient;
    private final AIConversationRepository conversationRepository;
    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();

    public AIService(LlmApiClient llmApiClient, AIConversationRepository conversationRepository) {
        this.llmApiClient = llmApiClient;
        this.conversationRepository = conversationRepository;
        logger.info("AIService inicializado com cliente LLM local");
    }

    public AskResponse askOriento(String prompt, String conversationId, Usuario usuario) {
        logger.info("Processando pergunta do usuário para o Oriento");
        logger.debug("Prompt recebido: {}", prompt);

        AIConversation conversation = resolveConversation(conversationId, usuario);
        String effectiveConversationId = String.valueOf(conversation.getIdConversa());

        List<ChatMessage> history = conversationHistory.computeIfAbsent(
                effectiveConversationId,
                id -> {
                    logger.debug("Criando nova sessão de conversa para o ID {}", id);
                    List<ChatMessage> initial = new ArrayList<>();
                    initial.add(ChatMessage.system(SYSTEM_INSTRUCTION));
                    return initial;
                });

        history.add(ChatMessage.user(prompt));

        logger.debug("Enviando requisição para o LLM local com conversa {}", effectiveConversationId);
        String resposta = llmApiClient.chat(List.copyOf(history));

        history.add(ChatMessage.assistant(resposta));

        logger.info("Resposta gerada pelo Oriento com sucesso. Tamanho da resposta: {} caracteres",
                resposta != null ? resposta.length() : 0);
        logger.debug("Resposta: {}", resposta);
        logger.trace("Histórico da conversa ({} mensagens)", history.size());

        return new AskResponse(effectiveConversationId, resposta);
    }

    private AIConversation resolveConversation(String conversationId, Usuario usuario) {
        if (!StringUtils.hasText(conversationId)) {
            UUID generatedConversationId = UUID.randomUUID();
            AIConversation persistida = conversationRepository.save(
                    new AIConversation(generatedConversationId, usuario));
            logger.debug("Nova conversa {} criada para o usuário {}", generatedConversationId,
                    usuario.getIdUsuario());
            return persistida;
        }

        AIConversation existing = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversa não encontrada para o ID informado"));

        if (!existing.pertenceAo(usuario)) {
            throw new AccessDeniedException("Conversa não pertence ao usuário autenticado");
        }

        return existing;
    }
}
