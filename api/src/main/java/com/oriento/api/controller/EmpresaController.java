package com.oriento.api.controller;

import com.oriento.api.model.Empresa;
import com.oriento.api.services.EmpresaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private static final Logger logger = LoggerFactory.getLogger(EmpresaController.class);

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    /** GET /api/empresas */
    @GetMapping
    public ResponseEntity<List<Empresa>> listarTodas() {
        logger.debug("GET /api/empresas");
        return ResponseEntity.ok(empresaService.listarTodas());
    }

    /** GET /api/empresas/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarPorId(@PathVariable Integer id) {
        logger.debug("GET /api/empresas/{}", id);
        return ResponseEntity.ok(empresaService.buscarPorId(id));
    }

    /** POST /api/empresas?idUsuario={uuid} */
    @PostMapping
    public ResponseEntity<Empresa> criar(@RequestBody Empresa empresa,
                                         @RequestParam UUID idUsuario) {
        logger.info("POST /api/empresas - usuário: {}", idUsuario);
        Empresa criada = empresaService.criar(empresa, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /** PUT /api/empresas/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Empresa> atualizar(@PathVariable Integer id,
                                              @RequestBody Empresa dadosNovos) {
        logger.info("PUT /api/empresas/{}", id);
        return ResponseEntity.ok(empresaService.atualizar(id, dadosNovos));
    }

    /** DELETE /api/empresas/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        logger.info("DELETE /api/empresas/{}", id);
        empresaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
