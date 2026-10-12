package com.oriento.api.controller;

import com.oriento.api.model.AlertaFinanceiro;
import com.oriento.api.services.AlertaFinanceiroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** GET /api/alertas */
    @GetMapping
    public ResponseEntity<List<AlertaFinanceiro>> listarTodos() {
        logger.debug("GET /api/alertas");
        return ResponseEntity.ok(alertaService.listarTodos());
    }

    /** GET /api/alertas/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<AlertaFinanceiro>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/alertas/empresa/{}", idEmpresa);
        return ResponseEntity.ok(alertaService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/alertas/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<AlertaFinanceiro> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/alertas/{}", id);
        return ResponseEntity.ok(alertaService.buscarPorId(id));
    }

    /** POST /api/alertas/empresa/{idEmpresa} */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<AlertaFinanceiro> criar(@RequestBody AlertaFinanceiro alerta,
                                                   @PathVariable Integer idEmpresa) {
        logger.info("POST /api/alertas/empresa/{}", idEmpresa);
        AlertaFinanceiro criado = alertaService.criar(alerta, idEmpresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/alertas/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<AlertaFinanceiro> atualizar(@PathVariable Integer id,
                                                       @RequestBody AlertaFinanceiro dadosNovos) {
        logger.info("PUT /api/alertas/{}", id);
        return ResponseEntity.ok(alertaService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/alertas/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/alertas/{}", id);
        alertaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
