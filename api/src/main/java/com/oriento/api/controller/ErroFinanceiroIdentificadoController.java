package com.oriento.api.controller;

import com.oriento.api.model.ErroFinanceiroIdentificado;
import com.oriento.api.services.ErroFinanceiroIdentificadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/erros-financeiros")
public class ErroFinanceiroIdentificadoController {

    private static final Logger logger = LoggerFactory.getLogger(ErroFinanceiroIdentificadoController.class);

    private final ErroFinanceiroIdentificadoService erroService;

    public ErroFinanceiroIdentificadoController(ErroFinanceiroIdentificadoService erroService) {
        this.erroService = erroService;
    }

    /** GET /api/erros-financeiros */
    @GetMapping
    public ResponseEntity<List<ErroFinanceiroIdentificado>> listarTodos() {
        logger.debug("GET /api/erros-financeiros");
        return ResponseEntity.ok(erroService.listarTodos());
    }

    /** GET /api/erros-financeiros/empresa/{idEmpresa} */
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<ErroFinanceiroIdentificado>> listarPorEmpresa(@PathVariable Integer idEmpresa) {
        logger.debug("GET /api/erros-financeiros/empresa/{}", idEmpresa);
        return ResponseEntity.ok(erroService.listarPorEmpresa(idEmpresa));
    }

    /** GET /api/erros-financeiros/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ErroFinanceiroIdentificado> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/erros-financeiros/{}", id);
        return ResponseEntity.ok(erroService.buscarPorId(id));
    }

    /**
     * POST /api/erros-financeiros/empresa/{idEmpresa}
     * Query param opcional: idConteudo (conteúdo educacional recomendado)
     */
    @PostMapping("/empresa/{idEmpresa}")
    public ResponseEntity<ErroFinanceiroIdentificado> criar(
            @RequestBody ErroFinanceiroIdentificado erro,
            @PathVariable Integer idEmpresa,
            @RequestParam(required = false) Integer idConteudo) {
        logger.info("POST /api/erros-financeiros/empresa/{}", idEmpresa);
        ErroFinanceiroIdentificado criado = erroService.criar(erro, idEmpresa, idConteudo);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /**
     * PUT /api/erros-financeiros/{id}
     * Query param opcional: idConteudo
     */
    @PutMapping("/{id}")
    public ResponseEntity<ErroFinanceiroIdentificado> atualizar(
            @PathVariable Integer id,
            @RequestBody ErroFinanceiroIdentificado dadosNovos,
            @RequestParam(required = false) Integer idConteudo) {
        logger.info("PUT /api/erros-financeiros/{}", id);
        return ResponseEntity.ok(erroService.atualizar(id, dadosNovos, idConteudo));
    }

    /** DELETE /api/erros-financeiros/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/erros-financeiros/{}", id);
        erroService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
