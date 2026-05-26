package com.oriento.api.controller;

import com.oriento.api.dto.CriarUsuarioDTO;
import com.oriento.api.dto.UsuarioCriadoResponse;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;
import com.oriento.api.model.enuns.Regimetributario;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.UsuarioRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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

/**
 * Endpoint p\u00fablico de cadastro. Cria, em uma \u00fanica transa\u00e7\u00e3o,
 * o {@link Usuario} e a {@link Empresa} associada \u2014 substitui o que antes
 * era feito pela trigger {@code handle_new_user} no Supabase.
 */
@RestController
@RequestMapping("/api/auth")
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             EmpresaRepository empresaRepository,
                             BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        logger.info("UsuarioController inicializado com sucesso");
    }

    @Transactional
    @PostMapping({"/cadastro", "/register"})
    public ResponseEntity<UsuarioCriadoResponse> novoUsuario(@Valid @RequestBody CriarUsuarioDTO dto) {
        logger.info("Cadastro recebido. Email: {}", maskEmail(dto.email()));

        String cnpjLimpo = somenteDigitos(dto.cnpj());

        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            logger.warn("E-mail j\u00e1 cadastrado: {}", maskEmail(dto.email()));
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(UsuarioCriadoResponse.erroJaCadastrado());
        }

        if (empresaRepository.existsByCnpj(cnpjLimpo)) {
            logger.warn("CNPJ j\u00e1 cadastrado: {}", maskCnpj(cnpjLimpo));
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(UsuarioCriadoResponse.erroJaCadastrado());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario = usuarioRepository.save(usuario);

        Empresa empresa = new Empresa();
        empresa.setUsuario(usuario);
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setCnpj(cnpjLimpo);
        empresa.setRazaoSocial(emptyToNull(dto.razaoSocial()));
        // regime_tributario \u00e9 NOT NULL no banco; default sensato para
        // cadastro inicial, ajust\u00e1vel depois na tela de perfil.
        empresa.setRegimeTributario(Regimetributario.SimplesNacional);
        empresa = empresaRepository.save(empresa);

        usuario.setEmpresa(empresa);

        logger.info("Cadastro conclu\u00eddo. UsuarioID: {}, EmpresaID: {}",
                usuario.getIdUsuario(), empresa.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioCriadoResponse.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UsuarioCriadoResponse> handleValidationException(MethodArgumentNotValidException ex) {
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dados inv\u00e1lidos fornecidos.");
        logger.warn("Valida\u00e7\u00e3o falhou no cadastro: {}", msg);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(UsuarioCriadoResponse.campoInvalido(msg));
    }

    private String somenteDigitos(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("\\D", "");
    }

    private String emptyToNull(String v) {
        if (v == null) {
            return null;
        }
        String trimmed = v.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "***@***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskCnpj(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) return "************";
        if (cnpj.length() < 4) return "************";
        return "********" + cnpj.substring(cnpj.length() - 4);
    }
}
