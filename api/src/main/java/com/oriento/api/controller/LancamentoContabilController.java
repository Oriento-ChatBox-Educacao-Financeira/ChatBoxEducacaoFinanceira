package com.oriento.api.controller;

import com.oriento.api.model.LancamentoContabil;
import com.oriento.api.services.LancamentoContabilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** GET /api/lancamentos */
    @GetMapping
    public ResponseEntity<List<LancamentoContabil>> listarTodos() {
        logger.debug("GET /api/lancamentos");
        return ResponseEntity.ok(lancamentoService.listarTodos());
    }

    /** GET /api/lancamentos/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<LancamentoContabil>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/lancamentos/empresa/{}", idEmpresa);
        return ResponseEntity.ok(lancamentoService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/lancamentos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<LancamentoContabil> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/lancamentos/{}", id);
        return ResponseEntity.ok(lancamentoService.buscarPorId(id));
    }

    /**
     * POST /api/lancamentos/empresa/{idEmpresa}
     * Query param opcional: idSimulacao
     */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<LancamentoContabil> criar(@RequestBody LancamentoContabil lancamento,
                                                     @PathVariable Integer idEmpresa,
                                                     @RequestParam(required = false) Integer idSimulacao) {
        logger.info("POST /api/lancamentos/empresa/{}", idEmpresa);
        LancamentoContabil criado = lancamentoService.criar(lancamento, idEmpresa, idSimulacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/lancamentos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<LancamentoContabil> atualizar(@PathVariable Integer id,
                                                         @RequestBody LancamentoContabil dadosNovos) {
        logger.info("PUT /api/lancamentos/{}", id);
        return ResponseEntity.ok(lancamentoService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/lancamentos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/lancamentos/{}", id);
        lancamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
