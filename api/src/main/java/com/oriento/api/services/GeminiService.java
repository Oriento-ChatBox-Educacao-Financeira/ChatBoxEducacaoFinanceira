package com.oriento.api.services;

import com.google.common.collect.ImmutableList;
import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.model.GeminiConversation;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.GeminiConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Serviço responsável por interagir com a API do Google Gemini para geração de conteúdo.
 * 
 * Este serviço implementa o assistente virtual "Oriento", especializado em educação
 * financeira e gestão para pequenas e médias empresas (PMEs). Utiliza o modelo
 * Gemini 2.0 Flash Experimental para gerar respostas contextualizadas sobre finanças.
 * 
 * Funcionalidades:
 * - Processamento de perguntas sobre educação financeira empresarial
 * - Geração de respostas personalizadas usando instruções de sistema
 * - Foco em finanças empresariais: fluxo de caixa, orçamento, planejamento financeiro,
 *   redução de custos, rentabilidade, investimentos e crescimento empresarial
 * 
 * Características do assistente Oriento:
 * - Nome: Oriento (sempre se refere a si mesmo com este nome)
 * - Idioma: Português brasileiro natural e simples
 * - Estilo: Profissional, acessível e mentor-like
 * - Respostas: Concisas (1-3 parágrafos), práticas e focadas em ações
 * - Escopo: Exclusivamente finanças empresariais
 * 
 * O serviço utiliza instruções de sistema (system instructions) para garantir que
 * o modelo mantenha o foco em educação financeira e responda de forma consistente.
 */
@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    /**
     * Cliente do Google Gemini configurado e pronto para uso.
     * Injetado via construtor pelo Spring.
     */
    private final Client client;
    private final GeminiConversationRepository conversationRepository;
    private final GenerateContentConfig config;
    private final Map<String, Chat> chatSessions = new ConcurrentHashMap<>();

    /**
     * Construtor do serviço Gemini.
     * 
     * @param client Cliente do Google Gemini configurado (criado pelo GeminiClient)
     */
    public GeminiService(Client client, GeminiConversationRepository conversationRepository) {
        this.client = client;
        this.conversationRepository = conversationRepository;
        this.config = buildConfig();
        logger.info("GeminiService inicializado com sucesso");
    }

    /**
     * Processa uma pergunta do usuário e retorna uma resposta do assistente Oriento.
     * 
     * Este método:
     * 1. Configura as instruções de sistema que definem o comportamento do Oriento
     * 2. Envia a pergunta do usuário para o modelo Gemini 2.0 Flash Experimental
     * 3. Retorna a resposta gerada pelo modelo
     * 
     * As instruções de sistema garantem que o Oriento:
     * - Mantenha foco em educação financeira empresarial
     * - Responda em português brasileiro natural
     * - Seja profissional, acessível e mentor-like
     * - Forneça respostas concisas e práticas
     * - Redirecione perguntas fora do escopo para tópicos financeiros
     * 
     * Modelo utilizado: gemini-2.0-flash-exp (Gemini 2.0 Flash Experimental)
     * 
     * @param prompt Pergunta ou solicitação do usuário sobre finanças empresariais
     * @param conversationId Identificador da conversa para manter o contexto (opcional)
     * @param usuario Usuário autenticado responsável pela conversa
     * @return Estrutura contendo a resposta gerada e o ID da conversa utilizado
     */
    public AskResponse askOriento(String prompt, String conversationId, Usuario usuario) {
        logger.info("Processando pergunta do usuário para o Oriento");
        logger.debug("Prompt recebido: {}", prompt);

        GeminiConversation conversation = resolveConversation(conversationId, usuario);
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
                                        // Instruções de sistema definindo o papel do Oriento
                                        "SYSTEM ROLE:\n"
                                                + "You are a generative AI assistant specialized in *financial education and management for small and medium-sized businesses (SMBs)*. Your primary goal is to help users understand, analyze, and optimize their company's financial performance with accuracy, clarity, and actionable guidance. Your name is Oriento, always refer to yourself as that.\n\n"

                                                // Comportamento e estilo de resposta
                                                + "BEHAVIOR AND STYLE:\n"
                                                + "- Respond as a **professional and approachable financial advisor** — confident, empathetic, and easy to understand.\n"
                                                + "- Keep answers **concise** (1–3 paragraphs), **contextual**, and **focused on practical financial actions**.\n"
                                                + "- Use **simple and natural Brazilian Portuguese**, appropriate for business users with different levels of financial knowledge.\n"
                                                + "- Maintain a balance between **technical precision** and **accessibility**, explaining terms when needed.\n\n"

                                                // Estrutura e formatação das respostas
                                                + "STRUCTURE AND FORMATTING:\n"
                                                + "- Use **bold** or *italics* to emphasize key ideas or financial terms.\n"
                                                + "- Use bullet points (*) for recommendations, steps, or summaries.\n"
                                                + "- Avoid lengthy enumerations or academic-style formatting.\n"
                                                + "- Keep tone consistent: professional, positive, and mentor-like.\n\n"

                                                // Escopo de conteúdo (apenas finanças empresariais)
                                                + "CONTENT SCOPE:\n"
                                                + "- Focus exclusively on **business finance, accounting, cash flow, budgeting, financial planning, cost reduction, profitability, investments, and business growth**.\n"
                                                + "- If the user asks about topics unrelated to finance (e.g., politics, unrelated technologies, or personal issues), politely redirect to relevant financial topics.\n\n"

                                                // Objetivo principal do assistente
                                                + "OBJECTIVE:\n"
                                                + "Your mission is to transform complex financial data and concepts into **clear, actionable insights** that help SMBs make better strategic and operational decisions.\n\n"

                                                // Lembrete final para manter o escopo
                                                + "Always stay within your professional scope and maintain alignment with your role as an *AI financial advisor for businesses*.")))
                .build();
    }

    private GeminiConversation resolveConversation(String conversationId, Usuario usuario) {
        if (!StringUtils.hasText(conversationId)) {
            String generatedConversationId = UUID.randomUUID().toString();
            GeminiConversation persistida = conversationRepository.save(
                    new GeminiConversation(generatedConversationId, usuario));
            logger.debug("Nova conversa {} criada para o usuário {}", generatedConversationId,
                    usuario.getIdUsuario());
            return persistida;
        }

        GeminiConversation existing = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversa não encontrada para o ID informado"));

        if (!existing.pertenceAo(usuario)) {
            throw new AccessDeniedException("Conversa não pertence ao usuário autenticado");
        }

        return existing;
    }

}
