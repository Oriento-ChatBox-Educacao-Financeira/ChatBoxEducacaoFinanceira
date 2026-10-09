package com.oriento.api.controller;

import com.oriento.api.dto.dashboards.DashboardBalancoResponse;
import com.oriento.api.dto.dashboards.DashboardDreResponse;
import com.oriento.api.dto.dashboards.DashboardFluxoResponse;
import com.oriento.api.dto.dashboards.DashboardVisaoGeralResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.CurrentUserService;
import com.oriento.api.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

    private final DashboardService service;
    private final CurrentUserService currentUser;

    public DashboardController(DashboardService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/dre")
    public ResponseEntity<DashboardDreResponse> dre() {
        Usuario u = currentUser.obterUsuarioAutenticado();
        return ResponseEntity.ok(service.dre(u));
    }

    @GetMapping("/balanco")
    public ResponseEntity<DashboardBalancoResponse> balanco() {
        Usuario u = currentUser.obterUsuarioAutenticado();
        return ResponseEntity.ok(service.balanco(u));
    }

    @GetMapping("/fluxo-caixa")
    public ResponseEntity<DashboardFluxoResponse> fluxoCaixa() {
        Usuario u = currentUser.obterUsuarioAutenticado();
        return ResponseEntity.ok(service.fluxoCaixa(u));
    }

    @GetMapping("/visao-geral")
    public ResponseEntity<DashboardVisaoGeralResponse> visaoGeral() {
        Usuario u = currentUser.obterUsuarioAutenticado();
        return ResponseEntity.ok(service.visaoGeral(u));
    }
}
