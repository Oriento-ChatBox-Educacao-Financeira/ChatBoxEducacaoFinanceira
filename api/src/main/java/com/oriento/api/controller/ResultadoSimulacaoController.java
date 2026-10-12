package com.oriento.api.controller;

import com.oriento.api.model.ResultadoSimulacao;
import com.oriento.api.services.ResultadoSimulacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resultados-simulacao")
public class ResultadoSimulacaoController {

    private static final Logger logger = LoggerFactory.getLogger(ResultadoSimulacaoController.class);

    private final ResultadoSimulacaoService resultadoService;

    public ResultadoSimulacaoController(ResultadoSimulacaoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    /** GET /api/resultados-simulacao */
    @GetMapping
    public ResponseEntity<List<ResultadoSimulacao>> listarTodos() {
        logger.debug("GET /api/resultados-simulacao");
        return ResponseEntity.ok(resultadoService.listarTodos());
    }

    /** GET /api/resultados-simulacao/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ResultadoSimulacao> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/resultados-simulacao/{}", id);
        return ResponseEntity.ok(resultadoService.buscarPorId(id));
    }

    /** GET /api/resultados-simulacao/simulacao/{idSimulacao} */
    @GetMapping("/simulacao/{idSimulacao}")
    public ResponseEntity<ResultadoSimulacao> buscarPorSimulacao(@PathVariable Integer idSimulacao) {
        logger.debug("GET /api/resultados-simulacao/simulacao/{}", idSimulacao);
        return ResponseEntity.ok(resultadoService.buscarPorSimulacao(idSimulacao));
    }

    /** POST /api/resultados-simulacao/simulacao/{idSimulacao} */
    @PostMapping("/simulacao/{idSimulacao}")
    public ResponseEntity<ResultadoSimulacao> criar(@RequestBody ResultadoSimulacao resultado,
                                                     @PathVariable Integer idSimulacao) {
        logger.info("POST /api/resultados-simulacao/simulacao/{}", idSimulacao);
        ResultadoSimulacao criado = resultadoService.criar(resultado, idSimulacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/resultados-simulacao/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ResultadoSimulacao> atualizar(@PathVariable Integer id,
                                                         @RequestBody ResultadoSimulacao dadosNovos) {
        logger.info("PUT /api/resultados-simulacao/{}", id);
        return ResponseEntity.ok(resultadoService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/resultados-simulacao/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/resultados-simulacao/{}", id);
        resultadoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
