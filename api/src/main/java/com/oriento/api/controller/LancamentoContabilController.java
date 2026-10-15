package com.oriento.api.controller;

import com.oriento.api.model.LancamentoContabil;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.LancamentoContabilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoContabilController {

    private static final Logger logger = LoggerFactory.getLogger(LancamentoContabilController.class);

    private final LancamentoContabilService lancamentoService;

    public LancamentoContabilController(LancamentoContabilService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    /** GET /api/lancamentos - Lista lançamentos da empresa autenticada */
    @GetMapping
    public ResponseEntity<List<LancamentoContabil>> listarTodos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/lancamentos - empresa: {}", usuario.empresaId());
        return ResponseEntity.ok(lancamentoService.listarPorEmpresa(usuario.empresaId()));
    }

    /** GET /api/lancamentos/{id} - Busca lançamento verificando posse da empresa */
    @GetMapping("/{id}")
    public ResponseEntity<LancamentoContabil> buscarPorId(@PathVariable Integer id,
                                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/lancamentos/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(lancamentoService.buscarPorId(id, usuario.empresaId()));
    }

    /**
     * POST /api/lancamentos
     * Cria lançamento vinculado à empresa do token JWT.
     * Query param opcional: idSimulacao
     */
    @PostMapping
    public ResponseEntity<LancamentoContabil> criar(@RequestBody LancamentoContabil lancamento,
                                                    @RequestParam(required = false) Integer idSimulacao,
                                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/lancamentos - empresa: {}", usuario.empresaId());
        LancamentoContabil criado = lancamentoService.criar(lancamento, usuario.empresaId(), idSimulacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/lancamentos/{id} - Atualiza lançamento verificando posse da empresa */
    @PutMapping("/{id}")
    public ResponseEntity<LancamentoContabil> atualizar(@PathVariable Integer id,
                                                        @RequestBody LancamentoContabil dadosNovos,
                                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PUT /api/lancamentos/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(lancamentoService.atualizar(id, dadosNovos, usuario.empresaId()));
    }

    /** DELETE /api/lancamentos/{id} - Deleta lançamento verificando posse da empresa */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/lancamentos/{} - empresa: {}", id, usuario.empresaId());
        lancamentoService.deletar(id, usuario.empresaId());
        return ResponseEntity.noContent().build();
    }
}
