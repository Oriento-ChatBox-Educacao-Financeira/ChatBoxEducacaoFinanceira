package com.oriento.api.services;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oriento.api.dto.UsuarioResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        logger.debug("UsuarioService inicializado com sucesso");
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        logger.info("Criando novo usuário. Email: {}", maskEmail(request.email()));

        // Validação: email único
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            logger.warn("Tentativa de cadastro com email já existente: {}", maskEmail(request.email()));
            throw new IllegalArgumentException("Email já cadastrado: " + request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        if (request.nivelMaturidadeFinanceira() != null) {
            usuario.setNivelMaturidade(request.nivelMaturidadeFinanceira().name());
        }

        Usuario salvo = usuarioRepository.save(usuario);
        logger.info("Usuário criado com sucesso. ID: {}", salvo.getIdUsuario());

        return UsuarioResponse.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        logger.debug("Listando todos os usuários");

        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        logger.debug("Buscando usuário por ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Usuário não encontrado. ID: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });

        return UsuarioResponse.fromEntity(usuario);
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioRequest request) {
        logger.info("Atualizando usuário ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Usuário não encontrado para atualização. ID: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });

        // Validação: novo email não pode pertencer a outro usuário
        if (!usuario.getEmail().equals(request.email())) {
            if (usuarioRepository.findByEmail(request.email()).isPresent()) {
                logger.warn("Email já em uso por outro usuário: {}", maskEmail(request.email()));
                throw new IllegalArgumentException("Email já cadastrado: " + request.email());
            }
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());

        // Atualiza senha apenas se um novo valor foi informado
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
            logger.debug("Senha atualizada para o usuário ID: {}", id);
        }

        if (request.nivelMaturidadeFinanceira() != null) {
            usuario.setNivelMaturidade(request.nivelMaturidadeFinanceira().name());
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        logger.info("Usuário atualizado com sucesso. ID: {}", atualizado.getIdUsuario());

        return UsuarioResponse.fromEntity(atualizado);
    }

    @Transactional
    public void deletar(UUID id) {
        logger.info("Deletando usuário ID: {}", id);

        if (!usuarioRepository.existsById(id)) {
            logger.warn("Tentativa de deletar usuário inexistente. ID: {}", id);
            throw new IllegalArgumentException("Usuário não encontrado com ID: " + id);
        }

        usuarioRepository.deleteById(id);
        logger.info("Usuário deletado com sucesso. ID: {}", id);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***";
        int at = email.indexOf("@");
        if (at <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}