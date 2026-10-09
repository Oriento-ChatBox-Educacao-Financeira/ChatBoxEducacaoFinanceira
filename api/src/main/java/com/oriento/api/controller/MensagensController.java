package com.oriento.api.controller;

import com.oriento.api.model.Mensagens;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.AIService;
import com.oriento.api.services.CurrentUserService;
import com.oriento.api.services.MensagensService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mensagens")
public class MensagensController {

    private static final Logger logger = LoggerFactory.getLogger(MensagensController.class);

    private final MensagensService mensagensService;
    private final AIService aiService;
    private final CurrentUserService currentUser;

    public MensagensController(MensagensService mensagensService,
                               AIService aiService,
                               CurrentUserService currentUser) {
        this.mensagensService = mensagensService;
        this.aiService = aiService;
        this.currentUser = currentUser;
    }

    /**
     * POST /api/mensagens
     * Salva uma nova mensagem no histórico da conversa (MongoDB).
     */
    @PostMapping
    public ResponseEntity<Mensagens> salvar(@RequestBody Mensagens mensagem) {
        logger.info("POST /api/mensagens");
        Mensagens salva = mensagensService.salvarMensagens(mensagem);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    /**
     * GET /api/mensagens/conversa/{conversaId}
     * Retorna o histórico de mensagens de uma conversa, ordenado por ordem ASC.
     * Apenas o dono da conversa pode acessá-la.
     */
    @GetMapping("/conversa/{conversaId}")
    public ResponseEntity<List<Mensagens>> buscarHistorico(@PathVariable UUID conversaId) {
        Usuario usuario = currentUser.obterUsuarioAutenticado();
        logger.debug("GET /api/mensagens/conversa/{} para usuário {}", conversaId, usuario.getIdUsuario());

        if (!aiService.conversaPertenceAoUsuario(conversaId, usuario)) {
            logger.warn("Usuário {} tentou acessar conversa {} sem permissão",
                    usuario.getIdUsuario(), conversaId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Conversa não pertence ao usuário autenticado");
        }

        return ResponseEntity.ok(mensagensService.buscarHistoricoDaConversa(conversaId));
    }

    /**
     * PATCH /api/mensagens/{id}/feedback
     * Registra o feedback do usuário sobre uma mensagem.
     * Body: { "util": true, "comentario": "..." }
     */
    @PatchMapping("/{id}/feedback")
    public ResponseEntity<Mensagens> atualizarFeedback(@PathVariable String id,
                                                        @RequestBody Map<String, Object> body) {
        logger.info("PATCH /api/mensagens/{}/feedback", id);
        Boolean util = (Boolean) body.get("util");
        String comentario = (String) body.get("comentario");
        return ResponseEntity.ok(mensagensService.atualizarFeedback(id, util, comentario));
    }

    /**
     * DELETE /api/mensagens/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        logger.info("DELETE /api/mensagens/{}", id);
        mensagensService.deletarMensagens(id);
        return ResponseEntity.noContent().build();
    }
}
