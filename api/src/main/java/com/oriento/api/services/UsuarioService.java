package com.oriento.api.services;


import com.oriento.api.dto.UsuarioResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo CRUD da entidade Usuario.
 *
 * Funcionalidades:
 * - Listar todos os usuários
 * - Buscar usuário por ID
 * - Criar novo usuário (com hash de senha)
 * - Atualizar dados do usuário
 * - Deletar usuário
 *
 * Observação: autenticação e geração de tokens JWT ficam no AuthService.
 */
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

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Cria um novo usuário no sistema.
     *
     * Regras:
     * - Email deve ser único
     * - CNPJ deve ser único (se informado)
     * - Senha é armazenada como hash BCrypt
     *
     * @param request DTO com os dados do novo usuário
     * @return UsuarioResponse com os dados persistidos
     * @throws IllegalArgumentException se email ou CNPJ já estiverem cadastrados
     */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        logger.info("Criando novo usuário. Email: {}", maskEmail(request.email()));

        // Validação: email único
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            logger.warn("Tentativa de cadastro com email já existente: {}", maskEmail(request.email()));
            throw new IllegalArgumentException("Email já cadastrado: " + request.email());
        }

        // Validação: CNPJ único (se informado)
        if (request.cnpj() != null && !request.cnpj().isBlank()) {
            if (usuarioRepository.findByCnpj(request.cnpj()).isPresent()) {
                logger.warn("Tentativa de cadastro com CNPJ já existente");
                throw new IllegalArgumentException("CNPJ já cadastrado.");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setCnpj(request.cnpj());
        usuario.setRazaoSocial(request.razaoSocial());
        usuario.setNomeFantasia(request.nomeFantasia());

        if (request.nivelMaturidadeFinanceira() != null) {
            usuario.setNivelMaturidadeUser(request.nivelMaturidadeFinanceira());
        }

        Usuario salvo = usuarioRepository.save(usuario);
        logger.info("Usuário criado com sucesso. ID: {}", salvo.getIdUsuario());

        return UsuarioResponse.fromEntity(salvo);
    }

    // ── READ (todos) ──────────────────────────────────────────────────────────

    /**
     * Lista todos os usuários cadastrados.
     *
     * @return Lista de UsuarioResponse
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        logger.debug("Listando todos os usuários");

        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── READ (por ID) ─────────────────────────────────────────────────────────

    /**
     * Busca um usuário pelo seu ID (UUID).
     *
     * @param id UUID do usuário
     * @return UsuarioResponse com os dados encontrados
     * @throws IllegalArgumentException se o usuário não for encontrado
     */
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

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Atualiza os dados de um usuário existente.
     *
     * Regras:
     * - Não permite alterar o email para um já cadastrado em outro usuário
     * - Senha só é atualizada se um novo valor for informado
     *
     * @param id      UUID do usuário a ser atualizado
     * @param request DTO com os novos dados
     * @return UsuarioResponse com os dados atualizados
     * @throws IllegalArgumentException se o usuário não for encontrado ou email já estiver em uso
     */
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
        usuario.setCnpj(request.cnpj());
        usuario.setRazaoSocial(request.razaoSocial());
        usuario.setNomeFantasia(request.nomeFantasia());

        // Atualiza senha apenas se um novo valor foi informado
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
            logger.debug("Senha atualizada para o usuário ID: {}", id);
        }

        if (request.nivelMaturidadeFinanceira() != null) {
            usuario.setNivelMaturidadeUser(request.nivelMaturidadeFinanceira());
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        logger.info("Usuário atualizado com sucesso. ID: {}", atualizado.getIdUsuario());

        return UsuarioResponse.fromEntity(atualizado);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Remove um usuário do sistema pelo seu ID.
     *
     * @param id UUID do usuário a ser removido
     * @throws IllegalArgumentException se o usuário não for encontrado
     */
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

    // ── Utilitários ───────────────────────────────────────────────────────────

    /** Mascara email para logs (ex: j***@email.com) */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***";
        int at = email.indexOf("@");
        if (at <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}