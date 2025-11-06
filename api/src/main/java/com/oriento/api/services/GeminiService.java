package com.oriento.api.services;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    /**
     * Construtor do serviço Gemini.
     * 
     * @param client Cliente do Google Gemini configurado (criado pelo GeminiClient)
     */
    public GeminiService(Client client) {
        this.client = client;
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
     * @return Resposta gerada pelo assistente Oriento em formato de texto
     */
    public String askOriento(String prompt) {
        logger.info("Processando pergunta do usuário para o Oriento");
        logger.debug("Prompt recebido: {}", prompt);
        
        // Configura as instruções de sistema que definem o comportamento do Oriento
        // Essas instruções garantem que o modelo mantenha foco em educação financeira
        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .systemInstruction(
                                Content.fromParts(
                                        Part.fromText(
                                                // Instruções de sistema definindo o papel do Oriento
                                                "SYSTEM ROLE:\n" +
                                                        "You are a generative AI assistant specialized in *financial education and management for small and medium-sized businesses (SMBs)*. Your primary goal is to help users understand, analyze, and optimize their company's financial performance with accuracy, clarity, and actionable guidance. Your name is Oriento, always refer to yourself as that.\n\n" +
                                                        
                                                        // Comportamento e estilo de resposta
                                                        "BEHAVIOR AND STYLE:\n" +
                                                        "- Respond as a **professional and approachable financial advisor** — confident, empathetic, and easy to understand.\n" +
                                                        "- Keep answers **concise** (1–3 paragraphs), **contextual**, and **focused on practical financial actions**.\n" +
                                                        "- Use **simple and natural Brazilian Portuguese**, appropriate for business users with different levels of financial knowledge.\n" +
                                                        "- Maintain a balance between **technical precision** and **accessibility**, explaining terms when needed.\n\n" +
                                                        
                                                        // Estrutura e formatação das respostas
                                                        "STRUCTURE AND FORMATTING:\n" +
                                                        "- Use **bold** or *italics* to emphasize key ideas or financial terms.\n" +
                                                        "- Use bullet points (*) for recommendations, steps, or summaries.\n" +
                                                        "- Avoid lengthy enumerations or academic-style formatting.\n" +
                                                        "- Keep tone consistent: professional, positive, and mentor-like.\n\n" +
                                                        
                                                        // Escopo de conteúdo (apenas finanças empresariais)
                                                        "CONTENT SCOPE:\n" +
                                                        "- Focus exclusively on **business finance, accounting, cash flow, budgeting, financial planning, cost reduction, profitability, investments, and business growth**.\n" +
                                                        "- If the user asks about topics unrelated to finance (e.g., politics, unrelated technologies, or personal issues), politely redirect to relevant financial topics.\n\n" +
                                                        
                                                        // Objetivo principal do assistente
                                                        "OBJECTIVE:\n" +
                                                        "Your mission is to transform complex financial data and concepts into **clear, actionable insights** that help SMBs make better strategic and operational decisions.\n\n" +
                                                        
                                                        // Lembrete final para manter o escopo
                                                        "Always stay within your professional scope and maintain alignment with your role as an *AI financial advisor for businesses*."
                                        )))
                        .build();
        
        logger.debug("Enviando requisição para o modelo Gemini 2.0 Flash Experimental");
        
        // Envia a requisição para o modelo Gemini com o prompt do usuário e as configurações
        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.0-flash-exp", // Modelo Gemini 2.0 Flash Experimental
                        prompt,                 // Pergunta do usuário
                        config);                // Configurações com instruções de sistema
        
        String resposta = response.text();
        
        logger.info("Resposta gerada pelo Oriento com sucesso. Tamanho da resposta: {} caracteres", 
                resposta != null ? resposta.length() : 0);
        logger.debug("Resposta: {}", resposta);
        
        return resposta;
    }

}
