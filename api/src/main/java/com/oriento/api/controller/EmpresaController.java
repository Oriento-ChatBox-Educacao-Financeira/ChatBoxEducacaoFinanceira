package com.oriento.api.controller;

import com.oriento.api.model.Empresa;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.EmpresaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private static final Logger logger = LoggerFactory.getLogger(EmpresaController.class);

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    /** GET /api/empresas - Lista a empresa do usuário autenticado */
    @GetMapping
    public ResponseEntity<List<Empresa>> listarTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/empresas - usuário: {}", usuario.userId());
        return ResponseEntity.ok(empresaService.listarPorUsuario(usuario.userId()));
    }

    /** GET /api/empresas/{id} - Busca empresa verificando posse do usuário autenticado */
    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarPorId(@PathVariable Integer id,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/empresas/{} - usuário: {}", id, usuario.userId());
        return ResponseEntity.ok(empresaService.buscarPorId(id, usuario.userId()));
    }

    /**
     * POST /api/empresas
     * Cria empresa vinculada diretamente ao usuário autenticado pelo token JWT.
     */
    @PostMapping
    public ResponseEntity<Empresa> criar(@RequestBody Empresa empresa,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/empresas - usuário autenticado: {}", usuario.userId());
        Empresa criada = empresaService.criar(empresa, usuario.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /** PUT /api/empresas/{id} - Atualiza empresa verificando posse do usuário autenticado */
    @PutMapping("/{id}")
    public ResponseEntity<Empresa> atualizar(@PathVariable Integer id,
                                             @RequestBody Empresa dadosNovos,
                                             @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PUT /api/empresas/{} - usuário: {}", id, usuario.userId());
        return ResponseEntity.ok(empresaService.atualizar(id, dadosNovos, usuario.userId()));
    }

    /** DELETE /api/empresas/{id} - Deleta empresa verificando posse do usuário autenticado */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/empresas/{} - usuário: {}", id, usuario.userId());
        empresaService.deletar(id, usuario.userId());
        return ResponseEntity.noContent().build();
    }
}
