package com.oriento.api.controller;

import com.oriento.api.dto.CriarUsuarioDTO;
import com.oriento.api.dto.UsuarioCriadoResponse;
import com.oriento.api.model.Usuario;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável por gerenciar endpoints relacionados a usuários.
 * 
 * Endpoints disponíveis:
 * - POST /cadastro: Cria um novo usuário no sistema
 * 
 * Este controller lida com:
 * - Validação de dados de entrada (via Bean Validation)
 * - Verificação de duplicidade (email e CNPJ únicos)
 * - Criptografia de senhas usando BCrypt
 * - Tratamento de erros de validação
 * 
 * O endpoint de cadastro é público (não requer autenticação) conforme
 * configurado no AuthConfig.
 */
@RestController
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    /**
     * Repositório para acesso aos dados de usuários no banco de dados.
     */
    private final UsuarioRepository usuarioRepository;
    
    /**
     * Encoder BCrypt para criptografar senhas antes de armazenar no banco.
     */
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Construtor do controller de usuários.
     * 
     * @param usuarioRepository Repositório para acesso aos dados de usuários
     * @param passwordEncoder Encoder BCrypt para criptografia de senhas
     */
    public UsuarioController(UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        
        logger.info("UsuarioController inicializado com sucesso");
    }

    /**
     * Endpoint para cadastro de novos usuários.
     * 
     * Este endpoint realiza as seguintes validações e operações:
     * 1. Valida os dados de entrada usando Bean Validation (@Valid)
     * 2. Verifica se já existe usuário com o mesmo email
     * 3. Verifica se já existe usuário com o mesmo CNPJ
     * 4. Se não houver duplicidade, cria o novo usuário com senha criptografada
     * 5. Retorna resposta de sucesso ou erro apropriada
     * 
     * A senha é criptografada usando BCrypt antes de ser armazenada no banco,
     * garantindo que mesmo que o banco seja comprometido, as senhas não possam
     * ser recuperadas em texto plano.
     * 
     * O método é transacional, garantindo que todas as operações sejam executadas
     * atomicamente.
     * 
     * @param dto DTO contendo os dados do usuário a ser criado (nome, email, CNPJ, etc)
     * @return ResponseEntity com UsuarioCriadoResponse indicando sucesso ou erro
     */
    @Transactional
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioCriadoResponse> novoUsuario(@Valid @RequestBody CriarUsuarioDTO dto) {
        logger.info("Recebida requisição de cadastro de novo usuário. Email: {}, CNPJ: {}", 
                maskEmail(dto.email()), maskCnpj(dto.cnpj()));
        
        // Verifica se já existe usuário com o mesmo CNPJ
        var usuarioExisteCNPJ = usuarioRepository.findByCnpj(dto.cnpj());
        
        // Verifica se já existe usuário com o mesmo email
        var usuarioExisteEMAIL = usuarioRepository.findByEmail(dto.email());
        
        // Se já existe usuário com email ou CNPJ, retorna erro
        if(usuarioExisteEMAIL.isPresent() || usuarioExisteCNPJ.isPresent()) {
            String motivo = usuarioExisteEMAIL.isPresent() ? "email" : "CNPJ";
            logger.warn("Tentativa de cadastro com {} já cadastrado. Email: {}, CNPJ: {}", 
                    motivo, maskEmail(dto.email()), maskCnpj(dto.cnpj()));
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(UsuarioCriadoResponse.erroJaCadastrado());
        }
        
        logger.debug("Nenhum usuário duplicado encontrado. Criando novo usuário...");
        
        // Cria novo usuário
        var usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCnpj(dto.cnpj());
        usuario.setRazaoSocial(dto.razaoSocial());
        usuario.setNomeFantasia(dto.nomeFantasia());
        
        // Criptografa a senha antes de armazenar (BCrypt gera hash único a cada execução)
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        
        // Salva o usuário no banco de dados
        usuario = usuarioRepository.save(usuario);
        
        logger.info("Usuário criado com sucesso. ID: {}, Email: {}", 
                usuario.getIdUsuario(), maskEmail(usuario.getEmail()));
        
        return ResponseEntity.ok(UsuarioCriadoResponse.of());
    }

    /**
     * Tratador de exceções para erros de validação de dados.
     * 
     * Este método é chamado automaticamente pelo Spring quando uma exceção
     * MethodArgumentNotValidException é lançada, o que acontece quando
     * os dados fornecidos não passam nas validações do Bean Validation.
     * 
     * Extrai a primeira mensagem de erro dos campos validados e retorna
     * uma resposta HTTP 422 (Unprocessable Entity) com a mensagem de erro.
     * 
     * @param ex Exceção de validação lançada pelo Spring
     * @return ResponseEntity com UsuarioCriadoResponse contendo a mensagem de erro
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UsuarioCriadoResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Erro de validação nos dados fornecidos");
        
        // Extrai todos os erros de validação
        var errors = ex.getBindingResult().getFieldErrors();
        
        // Pega a primeira mensagem de erro (ou mensagem padrão se não houver)
        var errorMessage = errors.stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos fornecidos.");
        
        logger.debug("Mensagem de erro de validação: {}", errorMessage);
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(UsuarioCriadoResponse.campoInvalido(errorMessage));
    }

    /**
     * Mascara email para logs, ocultando parte do endereço por segurança.
     * 
     * @param email Email a ser mascarado
     * @return Email mascarado (ex: "u***@example.com")
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "***@***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Mascara CNPJ para logs, ocultando a maior parte do número por segurança.
     * 
     * @param cnpj CNPJ a ser mascarado
     * @return CNPJ mascarado (ex: "********1234")
     */
    private String maskCnpj(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) return "************";
        if (cnpj.length() < 4) return "************";
        return "********" + cnpj.substring(cnpj.length() - 4);
    }

}
