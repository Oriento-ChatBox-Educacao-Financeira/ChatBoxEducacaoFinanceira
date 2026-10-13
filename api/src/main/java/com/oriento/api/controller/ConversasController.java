package com.oriento.api.controller;

import com.oriento.api.model.Usuario;
import com.oriento.api.services.AIService;
import com.oriento.api.services.AIService.ConversaResumo;
import com.oriento.api.services.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversas")
public class ConversasController {

    private static final Logger logger = LoggerFactory.getLogger(ConversasController.class);

    private final AIService aiService;
    private final CurrentUserService currentUser;

    public ConversasController(AIService aiService, CurrentUserService currentUser) {
        this.aiService = aiService;
        this.currentUser = currentUser;
    }

    /**
     * Lista as conversas do usuário autenticado, ordenadas pela mais recente,
     * com título derivado da primeira pergunta enviada.
     */
    @GetMapping
    public ResponseEntity<List<ConversaResumo>> listar() {
        Usuario usuario = currentUser.obterUsuarioAutenticado();
        logger.debug("GET /api/conversas para usuário {}", usuario.getIdUsuario());
        return ResponseEntity.ok(aiService.listarConversas(usuario));
    }
}
