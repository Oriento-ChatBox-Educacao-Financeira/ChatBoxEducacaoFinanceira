package com.oriento.api.controller;

import com.oriento.api.model.PlanoConta;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.PlanoContaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planos-conta")
public class PlanoContaController {

    private static final Logger logger = LoggerFactory.getLogger(PlanoContaController.class);

    private final PlanoContaService planoContaService;

    public PlanoContaController(PlanoContaService planoContaService) {
        this.planoContaService = planoContaService;
    }

    /** GET /api/planos-conta - Lista planos de conta da empresa autenticada */
    @GetMapping
    public ResponseEntity<List<PlanoConta>> listarTodos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/planos-conta - empresa: {}", usuario.empresaId());
        return ResponseEntity.ok(planoContaService.listarPorEmpresa(usuario.empresaId()));
    }

    /** GET /api/planos-conta/{id} - Busca plano de conta verificando posse da empresa */
    @GetMapping("/{id}")
    public ResponseEntity<PlanoConta> buscarPorId(@PathVariable Integer id,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/planos-conta/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(planoContaService.buscarPorId(id, usuario.empresaId()));
    }

    /**
     * POST /api/planos-conta
     * Cria plano de conta vinculado à empresa do token JWT.
     * Query param opcional: idContaPai (para contas sintéticas)
     */
    @PostMapping
    public ResponseEntity<PlanoConta> criar(@RequestBody PlanoConta planoConta,
                                            @RequestParam(required = false) Integer idContaPai,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/planos-conta - empresa: {}", usuario.empresaId());
        PlanoConta criado = planoContaService.criar(planoConta, usuario.empresaId(), idContaPai);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /**
     * PUT /api/planos-conta/{id}
     * Query param opcional: idContaPai
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanoConta> atualizar(@PathVariable Integer id,
                                                @RequestBody PlanoConta dadosNovos,
                                                @RequestParam(required = false) Integer idContaPai,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PUT /api/planos-conta/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(planoContaService.atualizar(id, dadosNovos, idContaPai, usuario.empresaId()));
    }

    /** DELETE /api/planos-conta/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/planos-conta/{} - empresa: {}", id, usuario.empresaId());
        planoContaService.deletar(id, usuario.empresaId());
        return ResponseEntity.noContent().build();
    }
}
