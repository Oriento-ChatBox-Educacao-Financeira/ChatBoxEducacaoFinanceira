package com.oriento.api.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.oriento.api.dto.AskResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import com.oriento.api.services.AIService;

@RestController
@RequestMapping("/api/oriento")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private final AIService AIService;
    private final UsuarioRepository usuarioRepository;

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
