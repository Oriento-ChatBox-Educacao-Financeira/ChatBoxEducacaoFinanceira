package com.oriento.api.controller;

import com.oriento.api.model.PlanoConta;
import com.oriento.api.services.PlanoContaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** GET /api/planos-conta */
    @GetMapping
    public ResponseEntity<List<PlanoConta>> listarTodos() {
        logger.debug("GET /api/planos-conta");
        return ResponseEntity.ok(planoContaService.listarTodos());
    }

    /** GET /api/planos-conta/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<PlanoConta>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/planos-conta/empresa/{}", idEmpresa);
        return ResponseEntity.ok(planoContaService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/planos-conta/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PlanoConta> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/planos-conta/{}", id);
        return ResponseEntity.ok(planoContaService.buscarPorId(id));
    }

    /**
     * POST /api/planos-conta/empresa/{idEmpresa}
     * Query param opcional: idContaPai (para contas sintéticas)
     */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<PlanoConta> criar(@RequestBody PlanoConta planoConta,
                                             @PathVariable Integer idEmpresa,
                                             @RequestParam(required = false) Integer idContaPai) {
        logger.info("POST /api/planos-conta/empresa/{}", idEmpresa);
        PlanoConta criado = planoContaService.criar(planoConta, idEmpresa, idContaPai);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /**
     * PUT /api/planos-conta/{id}
     * Query param opcional: idContaPai
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanoConta> atualizar(@PathVariable Integer id,
                                                 @RequestBody PlanoConta dadosNovos,
                                                 @RequestParam(required = false) Integer idContaPai) {
        logger.info("PUT /api/planos-conta/{}", id);
        return ResponseEntity.ok(planoContaService.atualizar(id, dadosNovos, idContaPai));
    }

    /** DELETE /api/planos-conta/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/planos-conta/{}", id);
        planoContaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
