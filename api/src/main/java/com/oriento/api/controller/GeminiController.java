package com.oriento.api.controller;

import com.oriento.api.services.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsável por gerenciar endpoints relacionados ao assistente virtual Oriento.
 * 
 * O Oriento é um assistente de IA especializado em educação financeira e gestão
 * para pequenas e médias empresas (PMEs), utilizando a API do Google Gemini.
 * 
 * Endpoints disponíveis:
 * - POST /oriento/ask: Envia uma pergunta ao assistente Oriento e recebe uma resposta
 * 
 * Este controller atua como uma camada fina, delegando toda a lógica de processamento
 * para o GeminiService, mantendo a separação de responsabilidades.
 * 
 * O endpoint requer autenticação via JWT (configurado no AuthConfig), garantindo
 * que apenas usuários autenticados possam interagir com o assistente.
 */
@RestController
@RequestMapping("/oriento")
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    /**
     * Serviço que contém a lógica de interação com a API do Google Gemini.
     * Responsável por processar perguntas e gerar respostas do assistente Oriento.
     */
    private final GeminiService geminiService;

    /**
     * Construtor do controller do Gemini.
     * 
     * @param geminiService Serviço que contém a lógica de processamento do Oriento
     */
    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
        logger.info("GeminiController inicializado com sucesso");
    }

    /**
     * Endpoint para enviar perguntas ao assistente virtual Oriento.
     * 
     * Este endpoint recebe uma pergunta do usuário sobre finanças empresariais
     * e retorna uma resposta gerada pelo assistente Oriento, especializado em
     * educação financeira para pequenas e médias empresas.
     * 
     * NOTA: O parâmetro 'personalidade' está presente na assinatura do método
     * mas não é utilizado atualmente. Pode ser implementado no futuro para
     * personalizar o estilo de resposta do assistente.
     * 
     * @param prompt Pergunta ou solicitação do usuário sobre finanças empresariais
     * @param personalidade Parâmetro para personalização do estilo de resposta (não utilizado atualmente)
     * @return Resposta gerada pelo assistente Oriento em formato de texto
     */
    @PostMapping("/ask")
    public String askGeminiApi(@RequestBody String prompt, @RequestParam Integer personalidade) {
        logger.info("Recebida requisição para o assistente Oriento");
        logger.debug("Prompt: {}, Personalidade: {}", prompt, personalidade);
        
        // NOTA: O parâmetro 'personalidade' não está sendo utilizado atualmente
        // Pode ser implementado no futuro para personalizar o comportamento do Oriento
        if (personalidade != null) {
            logger.debug("Parâmetro de personalidade recebido: {} (não utilizado atualmente)", personalidade);
        }
        
        // Delega o processamento para o GeminiService
        // O serviço é responsável por toda a lógica de interação com a API do Gemini
        String resposta = geminiService.askOriento(prompt);
        
        logger.info("Resposta do Oriento gerada com sucesso");
        logger.debug("Tamanho da resposta: {} caracteres", resposta != null ? resposta.length() : 0);
        
        return resposta;
    }

}
