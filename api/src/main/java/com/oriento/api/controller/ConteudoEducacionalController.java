package com.oriento.api.controller;

import com.oriento.api.model.ConteudoEducacional;
import com.oriento.api.services.ConteudoEducacionalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conteudos")
public class ConteudoEducacionalController {

    private static final Logger logger = LoggerFactory.getLogger(ConteudoEducacionalController.class);

    private final ConteudoEducacionalService conteudoService;

    public ConteudoEducacionalController(ConteudoEducacionalService conteudoService) {
        this.conteudoService = conteudoService;
    }

    /** GET /api/conteudos */
    @GetMapping
    public ResponseEntity<List<ConteudoEducacional>> listarTodos() {
        logger.debug("GET /api/conteudos");
        return ResponseEntity.ok(conteudoService.listarTodos());
    }

    /** GET /api/conteudos/categoria/{categoria} */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ConteudoEducacional>> listarPorCategoria(@PathVariable String categoria) {
        logger.debug("GET /api/conteudos/categoria/{}", categoria);
        return ResponseEntity.ok(conteudoService.listarPorCategoria(categoria));
    }

    /** GET /api/conteudos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ConteudoEducacional> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/conteudos/{}", id);
        return ResponseEntity.ok(conteudoService.buscarPorId(id));
    }

    /** POST /api/conteudos */
    @PostMapping
    public ResponseEntity<ConteudoEducacional> criar(@RequestBody ConteudoEducacional conteudo) {
        logger.info("POST /api/conteudos");
        ConteudoEducacional criado = conteudoService.criar(conteudo);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/conteudos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ConteudoEducacional> atualizar(@PathVariable Integer id,
                                                          @RequestBody ConteudoEducacional dadosNovos) {
        logger.info("PUT /api/conteudos/{}", id);
        return ResponseEntity.ok(conteudoService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/conteudos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/conteudos/{}", id);
        conteudoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
