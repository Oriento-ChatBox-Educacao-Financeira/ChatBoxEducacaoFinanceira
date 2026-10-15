package com.oriento.api.controller;

import com.oriento.api.dto.EmpresaIndicadoresResponse;
import com.oriento.api.services.AuthenticatedUserService;
import com.oriento.api.services.InternalIndicadoresService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/empresas")
public class InternalEmpresaController {
    private final InternalIndicadoresService indicadores;
    private final AuthenticatedUserService users;

    public InternalEmpresaController(InternalIndicadoresService indicadores, AuthenticatedUserService users) {
        this.indicadores = indicadores;
        this.users = users;
    }

    @GetMapping("/{id}/indicadores")
    public EmpresaIndicadoresResponse get(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return indicadores.get(id, users.resolve(jwt));
    }
}
