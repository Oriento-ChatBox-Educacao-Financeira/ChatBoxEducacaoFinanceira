package com.oriento.api.controller;

import com.oriento.api.model.DemonstrativoOficial;
import com.oriento.api.services.DemonstrativoOficialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** GET /api/demonstrativos */
    @GetMapping
    public ResponseEntity<List<DemonstrativoOficial>> listarTodos() {
        logger.debug("GET /api/demonstrativos");
        return ResponseEntity.ok(demonstrativoService.listarTodos());
    }

    /** GET /api/demonstrativos/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<DemonstrativoOficial>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/demonstrativos/empresa/{}", idEmpresa);
        return ResponseEntity.ok(demonstrativoService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/demonstrativos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<DemonstrativoOficial> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/demonstrativos/{}", id);
        return ResponseEntity.ok(demonstrativoService.buscarPorId(id));
    }

    /** POST /api/demonstrativos/empresa/{idEmpresa} */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<DemonstrativoOficial> criar(@RequestBody DemonstrativoOficial demonstrativo,
                                                       @PathVariable Integer idEmpresa) {
        logger.info("POST /api/demonstrativos/empresa/{}", idEmpresa);
        DemonstrativoOficial criado = demonstrativoService.criar(demonstrativo, idEmpresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/demonstrativos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<DemonstrativoOficial> atualizar(@PathVariable Integer id,
                                                           @RequestBody DemonstrativoOficial dadosNovos) {
        logger.info("PUT /api/demonstrativos/{}", id);
        return ResponseEntity.ok(demonstrativoService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/demonstrativos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/demonstrativos/{}", id);
        demonstrativoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
