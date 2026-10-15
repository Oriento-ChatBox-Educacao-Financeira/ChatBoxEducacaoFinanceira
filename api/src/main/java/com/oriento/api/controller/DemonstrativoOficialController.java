package com.oriento.api.controller;

import com.oriento.api.model.DemonstrativoOficial;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.DemonstrativoOficialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demonstrativos")
public class DemonstrativoOficialController {

    private static final Logger logger = LoggerFactory.getLogger(DemonstrativoOficialController.class);

    private final DemonstrativoOficialService demonstrativoService;

    public DemonstrativoOficialController(DemonstrativoOficialService demonstrativoService) {
        this.demonstrativoService = demonstrativoService;
    }

    /** GET /api/demonstrativos - Lista demonstrativos da empresa autenticada */
    @GetMapping
    public ResponseEntity<List<DemonstrativoOficial>> listarTodos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/demonstrativos - empresa: {}", usuario.empresaId());
        return ResponseEntity.ok(demonstrativoService.listarPorEmpresa(usuario.empresaId()));
    }

    /** GET /api/demonstrativos/{id} - Busca demonstrativo verificando posse da empresa */
    @GetMapping("/{id}")
    public ResponseEntity<DemonstrativoOficial> buscarPorId(@PathVariable Integer id,
                                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/demonstrativos/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(demonstrativoService.buscarPorId(id, usuario.empresaId()));
    }

    /**
     * POST /api/demonstrativos
     * Cria demonstrativo vinculado à empresa do token JWT.
     */
    @PostMapping
    public ResponseEntity<DemonstrativoOficial> criar(@RequestBody DemonstrativoOficial demonstrativo,
                                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/demonstrativos - empresa: {}", usuario.empresaId());
        DemonstrativoOficial criado = demonstrativoService.criar(demonstrativo, usuario.empresaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/demonstrativos/{id} - Atualiza demonstrativo verificando posse da empresa */
    @PutMapping("/{id}")
    public ResponseEntity<DemonstrativoOficial> atualizar(@PathVariable Integer id,
                                                           @RequestBody DemonstrativoOficial dadosNovos,
                                                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PUT /api/demonstrativos/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(demonstrativoService.atualizar(id, dadosNovos, usuario.empresaId()));
    }

    /** DELETE /api/demonstrativos/{id} - Deleta demonstrativo verificando posse da empresa */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/demonstrativos/{} - empresa: {}", id, usuario.empresaId());
        demonstrativoService.deletar(id, usuario.empresaId());
        return ResponseEntity.noContent().build();
    }
}
