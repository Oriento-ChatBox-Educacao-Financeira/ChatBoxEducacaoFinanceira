package com.oriento.api.controller;

import com.oriento.api.model.Conversa;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.ConversaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversas")
public class ConversaController {

    private static final Logger logger = LoggerFactory.getLogger(ConversaController.class);

    private final ConversaService conversaService;

    public ConversaController(ConversaService conversaService) {
        this.conversaService = conversaService;
    }

    /** GET /api/conversas - Lista conversas da empresa do usuário autenticado */
    @GetMapping
    public ResponseEntity<List<Conversa>> listarTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/conversas - empresa: {}", usuario.empresaId());
        return ResponseEntity.ok(conversaService.listarPorEmpresa(usuario.empresaId()));
    }

    /** GET /api/conversas/{id} - Busca conversa verificando se pertence à empresa autenticada */
    @GetMapping("/{id}")
    public ResponseEntity<Conversa> buscarPorId(@PathVariable UUID id,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/conversas/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(conversaService.buscarPorId(id, usuario.empresaId()));
    }

    /**
     * POST /api/conversas
     * Cria conversa vinculando automaticamente o usuário e a empresa do token JWT.
     */
    @PostMapping
    public ResponseEntity<Conversa> criar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/conversas - usuário: {}, empresa: {}", usuario.userId(), usuario.empresaId());
        Conversa criada = conversaService.criar(usuario.userId(), usuario.empresaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /**
     * PATCH /api/conversas/{id}/encerrar
     * Encerra uma conversa ativa pertencente à empresa do usuário autenticado.
     */
    @PatchMapping("/{id}/encerrar")
    public ResponseEntity<Conversa> encerrar(@PathVariable UUID id,
                                             @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PATCH /api/conversas/{}/encerrar - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(conversaService.encerrar(id, usuario.empresaId()));
    }

    /** DELETE /api/conversas/{id} - Deleta conversa pertencente à empresa do usuário autenticado */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/conversas/{} - empresa: {}", id, usuario.empresaId());
        conversaService.deletar(id, usuario.empresaId());
        return ResponseEntity.noContent().build();
    }
}
