package com.oriento.api.controller;

import com.oriento.api.model.SaldoConta;
import com.oriento.api.services.SaldoContaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saldos-conta")
public class SaldoContaController {

    private static final Logger logger = LoggerFactory.getLogger(SaldoContaController.class);

    private final SaldoContaService saldoContaService;

    public SaldoContaController(SaldoContaService saldoContaService) {
        this.saldoContaService = saldoContaService;
    }

    /** GET /api/saldos-conta */
    @GetMapping
    public ResponseEntity<List<SaldoConta>> listarTodos() {
        logger.debug("GET /api/saldos-conta");
        return ResponseEntity.ok(saldoContaService.listarTodos());
    }

    /** GET /api/saldos-conta/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<SaldoConta>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/saldos-conta/empresa/{}", idEmpresa);
        return ResponseEntity.ok(saldoContaService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/saldos-conta/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<SaldoConta> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/saldos-conta/{}", id);
        return ResponseEntity.ok(saldoContaService.buscarPorId(id));
    }

    /**
     * POST /api/saldos-conta/empresa/{idEmpresa}
     * Query param obrigatório: idConta
     */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<SaldoConta> criar(@RequestBody SaldoConta saldoConta,
                                             @PathVariable Integer idEmpresa,
                                             @RequestParam Integer idConta) {
        logger.info("POST /api/saldos-conta/empresa/{}", idEmpresa);
        SaldoConta criado = saldoContaService.criar(saldoConta, idEmpresa, idConta);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/saldos-conta/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<SaldoConta> atualizar(@PathVariable Integer id,
                                                 @RequestBody SaldoConta dadosNovos) {
        logger.info("PUT /api/saldos-conta/{}", id);
        return ResponseEntity.ok(saldoContaService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/saldos-conta/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/saldos-conta/{}", id);
        saldoContaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
