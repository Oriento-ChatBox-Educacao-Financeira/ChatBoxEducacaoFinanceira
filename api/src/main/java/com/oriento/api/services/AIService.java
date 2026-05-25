package com.oriento.api.services;

import com.google.common.collect.ImmutableList;
import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.model.AIConversation;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.AIConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final Client client;
    private final AIConversationRepository conversationRepository;
    private final GenerateContentConfig config;
    private final Map<String, Chat> chatSessions = new ConcurrentHashMap<>();

    public AIService(Client client, AIConversationRepository conversationRepository) {
        this.client = client;
        this.conversationRepository = conversationRepository;
        this.config = buildConfig();
        logger.info("GeminiService inicializado com sucesso");
    }

    public AskResponse askOriento(String prompt, String conversationId, Usuario usuario) {
        logger.info("Processando pergunta do usuário para o Oriento");
        logger.debug("Prompt recebido: {}", prompt);

        AIConversation conversation = resolveConversation(conversationId, usuario);
        String effectiveConversationId = conversation.getConversationId();

        Chat chatSession = chatSessions.computeIfAbsent(
                effectiveConversationId,
                id -> {
                    logger.debug("Criando nova sessão de conversa para o ID {}", id);
                    return client.chats.create("gemini-2.5-flash", config);
                });

        logger.debug("Enviando requisição para o modelo Gemini 2.5 Flash com conversa {}", effectiveConversationId);
        GenerateContentResponse response = chatSession.sendMessage(prompt);

        String resposta = response.text();

        ImmutableList<Content> history = chatSession.getHistory(true);
        logger.trace("Histórico da conversa ({} mensagens)", history != null ? history.size() : 0);

        logger.info("Resposta gerada pelo Oriento com sucesso. Tamanho da resposta: {} caracteres",
                resposta != null ? resposta.length() : 0);
        logger.debug("Resposta: {}", resposta);

        return new AskResponse(effectiveConversationId, resposta);
    }

    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .systemInstruction(
                        Content.fromParts(
                                Part.fromText(
                                        "SYSTEM ROLE:\n"
                                                + "You are a generative AI assistant specialized in *financial education and management for small and medium-sized businesses (SMBs)*. Your primary goal is to help users understand, analyze, and optimize their company's financial performance with accuracy, clarity, and actionable guidance. Your name is Oriento, always refer to yourself as that.\n\n"

                                                + "BEHAVIOR AND STYLE:\n"
                                                + "- Respond as a **professional and approachable financial advisor** — confident, empathetic, and easy to understand.\n"
                                                + "- Keep answers **concise** (1–3 paragraphs), **contextual**, and **focused on practical financial actions**.\n"
                                                + "- Use **simple and natural Brazilian Portuguese**, appropriate for business users with different levels of financial knowledge.\n"
                                                + "- Maintain a balance between **technical precision** and **accessibility**, explaining terms when needed.\n\n"

                                                + "STRUCTURE AND FORMATTING:\n"
                                                + "- Use **bold** or *italics* to emphasize key ideas or financial terms.\n"
                                                + "- Use bullet points (*) for recommendations, steps, or summaries.\n"
                                                + "- Avoid lengthy enumerations or academic-style formatting.\n"
                                                + "- Keep tone consistent: professional, positive, and mentor-like.\n\n"

                                                + "CONTENT SCOPE:\n"
                                                + "- Focus exclusively on **business finance, accounting, cash flow, budgeting, financial planning, cost reduction, profitability, investments, and business growth**.\n"
                                                + "- If the user asks about topics unrelated to finance (e.g., politics, unrelated technologies, or personal issues), politely redirect to relevant financial topics.\n\n"

                                                + "OBJECTIVE:\n"
                                                + "Your mission is to transform complex financial data and concepts into **clear, actionable insights** that help SMBs make better strategic and operational decisions.\n\n"

                                                + "Always stay within your professional scope and maintain alignment with your role as an *AI financial advisor for businesses*.")))
                .build();
    }

    private @NonNull AIConversation resolveConversation(String conversationId, Usuario usuario) {
        if (!StringUtils.hasText(conversationId)) {
            String generatedConversationId = UUID.randomUUID().toString();
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
