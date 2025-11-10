package com.oriento.api.controller;

import com.oriento.api.dto.AskResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import com.oriento.api.services.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

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
@Tag(name = "Assistente Oriento", description = "Endpoints para interação com o assistente virtual de educação financeira")
@SecurityRequirement(name = "Bearer Authentication")
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    /**
     * Serviço que contém a lógica de interação com a API do Google Gemini.
     * Responsável por processar perguntas e gerar respostas do assistente Oriento.
     */
    private final GeminiService geminiService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Construtor do controller do Gemini.
     * 
     * @param geminiService Serviço que contém a lógica de processamento do Oriento
     */
    public GeminiController(GeminiService geminiService, UsuarioRepository usuarioRepository) {
        this.geminiService = geminiService;
        this.usuarioRepository = usuarioRepository;
        logger.info("GeminiController inicializado com sucesso");
    }

    /**
     * Endpoint para enviar perguntas ao assistente virtual Oriento.
     *
     * Este endpoint recebe uma pergunta do usuário sobre finanças empresariais
     * e retorna uma resposta gerada pelo assistente Oriento, especializado em
     * educação financeira para pequenas e médias empresas.
     * 
     * @param prompt Pergunta ou solicitação do usuário sobre finanças empresariais
     * @param conversationId Identificador da conversa para manter contexto (opcional)
     * @return Estrutura contendo a resposta gerada e o identificador da conversa
     */
    @Operation(
        summary = "Fazer pergunta ao Oriento",
        description = "Envia uma pergunta sobre educação financeira para o assistente virtual Oriento e recebe uma resposta personalizada gerada por IA"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Resposta gerada com sucesso"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado - Token JWT necessário"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro ao processar a pergunta com a API do Gemini"
        )
    })
    @PostMapping("/ask")
    public AskResponse askGeminiApi(
            @RequestBody String prompt,
            @RequestParam(required = false) String conversationId,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("Recebida requisição para o assistente Oriento");
        logger.debug("Prompt: {}", prompt);

        Usuario usuario = resolverUsuario(jwt);
        logger.debug("Usuário autenticado: {}", usuario.getIdUsuario());
        
        // Delega o processamento para o GeminiService
        // O serviço é responsável por toda a lógica de interação com a API do Gemini
        AskResponse resposta = geminiService.askOriento(prompt, conversationId, usuario);
        
        logger.info("Resposta do Oriento gerada com sucesso");
        logger.debug("ID da conversa utilizado: {}", resposta != null ? resposta.conversationId() : null);
        logger.debug("Tamanho da resposta: {} caracteres",
                resposta != null && resposta.response() != null ? resposta.response().length() : 0);
        
        return resposta;
    }

    private Usuario resolverUsuario(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }

        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
    }

}
