package com.oriento.api.controller;

import com.oriento.api.dto.AskResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import com.oriento.api.services.AIService;
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
 * para pequenas e médias empresas (PMEs), utilizando a API AI.
 *
 * Endpoints disponíveis:
 * - POST /api/oriento/ask: Envia uma pergunta ao assistente Oriento e recebe uma resposta
 *
 * Este controller atua como uma camada fina, delegando toda a lógica de processamento
 * para o AIService, mantendo a separação de responsabilidades.
 *
 * O endpoint requer autenticação via JWT (configurado no AuthConfig), garantindo
 * que apenas usuários autenticados possam interagir com o assistente.
 */
@RestController
@RequestMapping("/api/oriento")
@Tag(name = "Assistente Oriento", description = "Endpoints para interação com o assistente virtual de educação financeira")
@SecurityRequirement(name = "Bearer Authentication")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    /**
     * Serviço que contém a lógica de interação com a API do Google AI.
     * Responsável por processar perguntas e gerar respostas do assistente Oriento.
     */
    private final AIService AIService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Construtor do controller do AI.
     * 
     * @param AIService Serviço que contém a lógica de processamento do Oriento
     */
    public AIController(AIService AIService, UsuarioRepository usuarioRepository) {
        this.AIService = AIService;
        this.usuarioRepository = usuarioRepository;
        logger.info("AIController inicializado com sucesso");
    }

    @PostMapping("/ask")
    public AskResponse askAIApi(
            @RequestBody String prompt,
            @RequestParam(required = false) String conversationId,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("Recebida requisição para o assistente Oriento");
        logger.debug("Prompt: {}", prompt);

        Usuario usuario = resolverUsuario(jwt);
        logger.debug("Usuário autenticado: {}", usuario.getIdUsuario());

        AskResponse resposta = AIService.askOriento(prompt, conversationId, usuario);
        
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
