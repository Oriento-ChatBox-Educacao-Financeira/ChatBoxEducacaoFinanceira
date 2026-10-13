package com.oriento.api.controller;

import com.oriento.api.dto.NotificacaoResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.CurrentUserService;
import com.oriento.api.services.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;
    private final CurrentUserService currentUser;

    public NotificacaoController(NotificacaoService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoResponse>> listar(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        Usuario u = currentUser.obterUsuarioAutenticado();
        List<NotificacaoResponse> response = service.listarPorUsuario(u.getIdUsuario(), limit)
                .stream()
                .map(NotificacaoResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> marcarLida(@PathVariable("id") Long id) {
        Usuario u = currentUser.obterUsuarioAutenticado();
        service.marcarComoLida(u.getIdUsuario(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/marcar-todas-lidas")
    public ResponseEntity<Void> marcarTodasLidas() {
        Usuario u = currentUser.obterUsuarioAutenticado();
        service.marcarTodasLidas(u.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}
