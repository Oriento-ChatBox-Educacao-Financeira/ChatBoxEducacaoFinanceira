package com.oriento.api.controller;

import com.oriento.api.model.IndicadorFinanceiro;
import com.oriento.api.services.IndicadorFinanceiroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/indicadores")
public class IndicadorFinanceiroController {

    private static final Logger logger = LoggerFactory.getLogger(IndicadorFinanceiroController.class);

    private final IndicadorFinanceiroService indicadorService;

    public IndicadorFinanceiroController(IndicadorFinanceiroService indicadorService) {
        this.indicadorService = indicadorService;
    }

    /** GET /api/indicadores */
    @GetMapping
    public ResponseEntity<List<IndicadorFinanceiro>> listarTodos() {
        logger.debug("GET /api/indicadores");
        return ResponseEntity.ok(indicadorService.listarTodos());
    }

    /** GET /api/indicadores/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<IndicadorFinanceiro>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/indicadores/empresa/{}", idEmpresa);
        return ResponseEntity.ok(indicadorService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/indicadores/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<IndicadorFinanceiro> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/indicadores/{}", id);
        return ResponseEntity.ok(indicadorService.buscarPorId(id));
    }

    /** POST /api/indicadores/empresa/{idEmpresa} */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<IndicadorFinanceiro> criar(@RequestBody IndicadorFinanceiro indicador,
                                                      @PathVariable Integer idEmpresa) {
        logger.info("POST /api/indicadores/empresa/{}", idEmpresa);
        IndicadorFinanceiro criado = indicadorService.criar(indicador, idEmpresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/indicadores/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<IndicadorFinanceiro> atualizar(@PathVariable Integer id,
                                                          @RequestBody IndicadorFinanceiro dadosNovos) {
        logger.info("PUT /api/indicadores/{}", id);
        return ResponseEntity.ok(indicadorService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/indicadores/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/indicadores/{}", id);
        indicadorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
