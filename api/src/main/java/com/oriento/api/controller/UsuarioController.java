package com.oriento.api.controller;

import com.oriento.api.dto.CriarUsuarioDTO;
import com.oriento.api.dto.UsuarioCriadoResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        
        logger.info("UsuarioController inicializado com sucesso");
    }

    @Transactional
    @PostMapping({"/cadastro", "/register"})
    public ResponseEntity<UsuarioCriadoResponse> novoUsuario(@Valid @RequestBody @NonNull CriarUsuarioDTO dto) {
        logger.info("Recebida requisição de cadastro de novo usuário. Email: {}, CNPJ: {}", 
                maskEmail(dto.email()), maskCnpj(dto.cnpj()));

        var usuarioExisteCNPJ = usuarioRepository.findByCnpj(dto.cnpj());
        var usuarioExisteEMAIL = usuarioRepository.findByEmail(dto.email());

        if(usuarioExisteEMAIL.isPresent() || usuarioExisteCNPJ.isPresent()) {
            String motivo = usuarioExisteEMAIL.isPresent() ? "email" : "CNPJ";
            logger.warn("Tentativa de cadastro com {} já cadastrado. Email: {}, CNPJ: {}", 
                    motivo, maskEmail(dto.email()), maskCnpj(dto.cnpj()));
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(UsuarioCriadoResponse.erroJaCadastrado());
        }
        
        logger.debug("Nenhum usuário duplicado encontrado. Criando novo usuário...");

        var usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCnpj(dto.cnpj());
        usuario.setRazaoSocial(dto.razaoSocial());
        usuario.setNomeFantasia(dto.nomeFantasia());

        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        usuario = usuarioRepository.save(usuario);
        
        logger.info("Usuário criado com sucesso. ID: {}, Email: {}", 
                usuario.getIdUsuario(), maskEmail(usuario.getEmail()));
        
        return ResponseEntity.ok(UsuarioCriadoResponse.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UsuarioCriadoResponse> handleValidationException(@NonNull MethodArgumentNotValidException ex) {
        logger.warn("Erro de validação nos dados fornecidos");

        var errors = ex.getBindingResult().getFieldErrors();

        var errorMessage = errors.stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos fornecidos.");
        
        logger.debug("Mensagem de erro de validação: {}", errorMessage);
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(UsuarioCriadoResponse.campoInvalido(errorMessage));
    }

    private @NonNull String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "***@***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private @NonNull String maskCnpj(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) return "************";
        if (cnpj.length() < 4) return "************";
        return "********" + cnpj.substring(cnpj.length() - 4);
    }

}
