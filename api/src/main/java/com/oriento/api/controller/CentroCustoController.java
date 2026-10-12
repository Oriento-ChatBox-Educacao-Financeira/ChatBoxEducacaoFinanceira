package com.oriento.api.controller;

import com.oriento.api.model.CentroCusto;
import com.oriento.api.services.CentroCustoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centros-custo")
public class CentroCustoController {

    private static final Logger logger = LoggerFactory.getLogger(CentroCustoController.class);

    private final CentroCustoService centroCustoService;

    public CentroCustoController(CentroCustoService centroCustoService) {
        this.centroCustoService = centroCustoService;
    }

    /** GET /api/centros-custo */
    @GetMapping
    public ResponseEntity<List<CentroCusto>> listarTodos() {
        logger.debug("GET /api/centros-custo");
        return ResponseEntity.ok(centroCustoService.listarTodos());
    }

    /** GET /api/centros-custo/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<CentroCusto>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/centros-custo/empresa/{}", idEmpresa);
        return ResponseEntity.ok(centroCustoService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/centros-custo/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<CentroCusto> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/centros-custo/{}", id);
        return ResponseEntity.ok(centroCustoService.buscarPorId(id));
    }

    /** POST /api/centros-custo/empresa/{idEmpresa} */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<CentroCusto> criar(@RequestBody CentroCusto centroCusto,
                                              @PathVariable Integer idEmpresa) {
        logger.info("POST /api/centros-custo/empresa/{}", idEmpresa);
        CentroCusto criado = centroCustoService.criar(centroCusto, idEmpresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/centros-custo/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<CentroCusto> atualizar(@PathVariable Integer id,
                                                  @RequestBody CentroCusto dadosNovos) {
        logger.info("PUT /api/centros-custo/{}", id);
        return ResponseEntity.ok(centroCustoService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/centros-custo/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/centros-custo/{}", id);
        centroCustoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
