package com.oriento.api.controller;

import com.oriento.api.model.IndicadorFinanceiro;
import com.oriento.api.security.UsuarioAutenticado;
import com.oriento.api.services.IndicadorFinanceiroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /** GET /api/indicadores - Lista indicadores da empresa autenticada */
    @GetMapping
    public ResponseEntity<List<IndicadorFinanceiro>> listarTodos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/indicadores - empresa: {}", usuario.empresaId());
        return ResponseEntity.ok(indicadorService.listarPorEmpresa(usuario.empresaId()));
    }

    /** GET /api/indicadores/{id} - Busca indicador verificando posse da empresa */
    @GetMapping("/{id}")
    public ResponseEntity<IndicadorFinanceiro> buscarPorId(@PathVariable Integer id,
                                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.debug("GET /api/indicadores/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(indicadorService.buscarPorId(id, usuario.empresaId()));
    }

    /**
     * POST /api/indicadores
     * Cria indicador vinculado à empresa do token JWT.
     */
    @PostMapping
    public ResponseEntity<IndicadorFinanceiro> criar(@RequestBody IndicadorFinanceiro indicador,
                                                      @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("POST /api/indicadores - empresa: {}", usuario.empresaId());
        IndicadorFinanceiro criado = indicadorService.criar(indicador, usuario.empresaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/indicadores/{id} - Atualiza indicador verificando posse da empresa */
    @PutMapping("/{id}")
    public ResponseEntity<IndicadorFinanceiro> atualizar(@PathVariable Integer id,
                                                          @RequestBody IndicadorFinanceiro dadosNovos,
                                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("PUT /api/indicadores/{} - empresa: {}", id, usuario.empresaId());
        return ResponseEntity.ok(indicadorService.atualizar(id, dadosNovos, usuario.empresaId()));
    }

    /** DELETE /api/indicadores/{id} - Deleta indicador verificando posse da empresa */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        logger.info("DELETE /api/indicadores/{} - empresa: {}", id, usuario.empresaId());
        indicadorService.deletar(id, usuario.empresaId());
        return ResponseEntity.noContent().build();
    }
}
