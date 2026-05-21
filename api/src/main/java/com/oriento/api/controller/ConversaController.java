package com.oriento.api.controller;

import com.oriento.api.model.Conversa;
import com.oriento.api.services.ConversaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** GET /api/conversas */
    @GetMapping
    public ResponseEntity<List<Conversa>> listarTodas() {
        logger.debug("GET /api/conversas");
        return ResponseEntity.ok(conversaService.listarTodas());
    }

    /** GET /api/conversas/usuario/{idUsuario} */
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Conversa>> listarPorUsuario(@PathVariable UUID idUsuario) {
        logger.debug("GET /api/conversas/usuario/{}", idUsuario);
        return ResponseEntity.ok(conversaService.listarPorUsuario(idUsuario));
    }

    /** GET /api/conversas/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<Conversa>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/conversas/empresa/{}", idEmpresa);
        return ResponseEntity.ok(conversaService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/conversas/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Conversa> buscarPorId(@PathVariable UUID id) {
        logger.debug("GET /api/conversas/{}", id);
        return ResponseEntity.ok(conversaService.buscarPorId(id));
    }

    /**
     * POST /api/conversas
     * Query params obrigatórios: idUsuario, idEmpresa
     */
    @PostMapping
    public ResponseEntity<Conversa> criar(@RequestParam UUID idUsuario,
                                           @RequestParam Integer idEmpresa) {
        logger.info("POST /api/conversas - usuário: {}, empresa: {}", idUsuario, idEmpresa);
        Conversa criada = conversaService.criar(idUsuario, idEmpresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /**
     * PATCH /api/conversas/{id}/encerrar
     * Encerra uma conversa ativa.
     */
    @PatchMapping("/{id}/encerrar")
    public ResponseEntity<Conversa> encerrar(@PathVariable UUID id) {
        logger.info("PATCH /api/conversas/{}/encerrar", id);
        return ResponseEntity.ok(conversaService.encerrar(id));
    }

    /** DELETE /api/conversas/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        logger.info("DELETE /api/conversas/{}", id);
        conversaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
