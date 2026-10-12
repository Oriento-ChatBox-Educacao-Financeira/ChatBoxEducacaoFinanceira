package com.oriento.api.controller;

import com.oriento.api.model.ParametroSimulacao;
import com.oriento.api.services.ParametroSimulacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parametros-simulacao")
public class ParametroSimulacaoController {

    private static final Logger logger = LoggerFactory.getLogger(ParametroSimulacaoController.class);

    private final ParametroSimulacaoService parametroService;

    public ParametroSimulacaoController(ParametroSimulacaoService parametroService) {
        this.parametroService = parametroService;
    }

    /** GET /api/parametros-simulacao */
    @GetMapping
    public ResponseEntity<List<ParametroSimulacao>> listarTodos() {
        logger.debug("GET /api/parametros-simulacao");
        return ResponseEntity.ok(parametroService.listarTodos());
    }

    /** GET /api/parametros-simulacao/simulacao/{idSimulacao} */
    @GetMapping("/simulacao/{idSimulacao}")
    public ResponseEntity<List<ParametroSimulacao>> listarPorSimulacao(@PathVariable Integer idSimulacao) {
        logger.debug("GET /api/parametros-simulacao/simulacao/{}", idSimulacao);
        return ResponseEntity.ok(parametroService.listarPorSimulacao(idSimulacao));
    }

    /** GET /api/parametros-simulacao/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ParametroSimulacao> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/parametros-simulacao/{}", id);
        return ResponseEntity.ok(parametroService.buscarPorId(id));
    }

    /** POST /api/parametros-simulacao/simulacao/{idSimulacao} */
    @PostMapping("/simulacao/{idSimulacao}")
    public ResponseEntity<ParametroSimulacao> criar(@RequestBody ParametroSimulacao parametro,
                                                     @PathVariable Integer idSimulacao) {
        logger.info("POST /api/parametros-simulacao/simulacao/{}", idSimulacao);
        ParametroSimulacao criado = parametroService.criar(parametro, idSimulacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/parametros-simulacao/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ParametroSimulacao> atualizar(@PathVariable Integer id,
                                                         @RequestBody ParametroSimulacao dadosNovos) {
        logger.info("PUT /api/parametros-simulacao/{}", id);
        return ResponseEntity.ok(parametroService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/parametros-simulacao/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/parametros-simulacao/{}", id);
        parametroService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
