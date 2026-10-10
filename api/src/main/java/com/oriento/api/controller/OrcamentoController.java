package com.oriento.api.controller;

import com.oriento.api.model.Orcamento;
import com.oriento.api.services.OrcamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private static final Logger logger = LoggerFactory.getLogger(OrcamentoController.class);

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    /** GET /api/orcamentos */
    @GetMapping
    public ResponseEntity<List<Orcamento>> listarTodos() {
        logger.debug("GET /api/orcamentos");
        return ResponseEntity.ok(orcamentoService.listarTodos());
    }

    /** GET /api/orcamentos/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<Orcamento>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/orcamentos/empresa/{}", idEmpresa);
        return ResponseEntity.ok(orcamentoService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/orcamentos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Orcamento> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/orcamentos/{}", id);
        return ResponseEntity.ok(orcamentoService.buscarPorId(id));
    }

    /**
     * POST /api/orcamentos/empresa/{idEmpresa}
     * Query params: idConta (obrigatório), idCentroCusto (opcional)
     */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<Orcamento> criar(@RequestBody Orcamento orcamento,
                                            @PathVariable Integer idEmpresa,
                                            @RequestParam Integer idConta,
                                            @RequestParam(required = false) Integer idCentroCusto) {
        logger.info("POST /api/orcamentos/empresa/{}", idEmpresa);
        Orcamento criado = orcamentoService.criar(orcamento, idEmpresa, idConta, idCentroCusto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/orcamentos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Orcamento> atualizar(@PathVariable Integer id,
                                                @RequestBody Orcamento dadosNovos) {
        logger.info("PUT /api/orcamentos/{}", id);
        return ResponseEntity.ok(orcamentoService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/orcamentos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/orcamentos/{}", id);
        orcamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
