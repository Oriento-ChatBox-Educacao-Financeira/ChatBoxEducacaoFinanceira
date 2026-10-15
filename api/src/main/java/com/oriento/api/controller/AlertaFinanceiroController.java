package com.oriento.api.controller;

import com.oriento.api.model.AlertaFinanceiro;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.AlertaFinanceiroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaFinanceiroController {

    private static final Logger logger = LoggerFactory.getLogger(AlertaFinanceiroController.class);

    private final AlertaFinanceiroService alertaService;

    public AlertaFinanceiroController(AlertaFinanceiroService alertaService) {
        this.alertaService = alertaService;
    }

    /** GET /api/alertas - Lista alertas da empresa autenticada */
    @GetMapping
    public ResponseEntity<List<AlertaFinanceiro>> listarTodos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/alertas - empresa: {}", usuario.empresaId());
        return ResponseEntity.ok(alertaService.listarPorEmpresa(usuario.empresaId()));
    }

    /** GET /api/alertas/{id} - Busca alerta verificando posse da empresa */
    @GetMapping("/{id}")
    public ResponseEntity<AlertaFinanceiro> buscarPorId(@PathVariable Integer id,
                                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/alertas/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(alertaService.buscarPorId(id, usuario.empresaId()));
    }

    /**
     * POST /api/alertas
     * Cria alerta vinculado à empresa do token JWT.
     */
    @PostMapping
    public ResponseEntity<AlertaFinanceiro> criar(@RequestBody AlertaFinanceiro alerta,
                                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/alertas - empresa: {}", usuario.empresaId());
        AlertaFinanceiro criado = alertaService.criar(alerta, usuario.empresaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/alertas/{id} - Atualiza alerta verificando posse da empresa */
    @PutMapping("/{id}")
    public ResponseEntity<AlertaFinanceiro> atualizar(@PathVariable Integer id,
                                                       @RequestBody AlertaFinanceiro dadosNovos,
                                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PUT /api/alertas/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(alertaService.atualizar(id, dadosNovos, usuario.empresaId()));
    }

    /** DELETE /api/alertas/{id} - Deleta alerta verificando posse da empresa */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/alertas/{} - empresa: {}", id, usuario.empresaId());
        alertaService.deletar(id, usuario.empresaId());
        return ResponseEntity.noContent().build();
    }
}
