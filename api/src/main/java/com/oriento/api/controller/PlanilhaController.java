package com.oriento.api.controller;

import com.oriento.api.dto.PlanilhaImportResponse;
import com.oriento.api.dto.PlanilhaResumoResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.CurrentUserService;
import com.oriento.api.services.PlanilhaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/planilhas")
public class PlanilhaController {

    private final PlanilhaService service;
    private final CurrentUserService currentUser;

    public PlanilhaController(PlanilhaService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/upload")
    public ResponseEntity<PlanilhaImportResponse> upload(@RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        }
        Usuario u = currentUser.obterUsuarioAutenticado();
        PlanilhaImportResponse response = service.importar(u, arquivo);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PlanilhaResumoResponse>> listar() {
        Usuario u = currentUser.obterUsuarioAutenticado();
        return ResponseEntity.ok(service.listarPorUsuario(u));
    }

    @DeleteMapping("/{arquivo}")
    public ResponseEntity<Void> deletar(@PathVariable("arquivo") String arquivo) {
        Usuario u = currentUser.obterUsuarioAutenticado();
        service.deletar(u, arquivo);
        return ResponseEntity.noContent().build();
    }
}
