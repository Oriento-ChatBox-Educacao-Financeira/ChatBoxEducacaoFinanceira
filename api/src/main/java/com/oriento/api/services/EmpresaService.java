package com.oriento.api.services;

import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmpresaService {

    private static final Logger logger = LoggerFactory.getLogger(EmpresaService.class);

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpresaService(EmpresaRepository empresaRepository,
                          UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lista a empresa vinculada ao usuário autenticado (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public List<Empresa> listarPorUsuario(UUID idUsuario) {
        logger.debug("Listando empresa do usuário ID: {}", idUsuario);
        return empresaRepository.findByUsuarioIdUsuario(idUsuario)
                .map(List::of)
                .orElse(List.of());
    }

    /**
     * Busca empresa por ID verificando se pertence ao usuário autenticado (anti-IDOR).
     */
    @Transactional(readOnly = true)
    public Empresa buscarPorId(Integer id, UUID idUsuario) {
        logger.debug("Buscando empresa por ID: {} para usuário: {}", id, idUsuario);
        return empresaRepository.findByIdAndUsuarioIdUsuario(id, idUsuario)
                .orElseThrow(() -> {
                    logger.warn("Empresa não encontrada ou não pertence ao usuário. ID: {}", id);
                    return new IllegalArgumentException("Empresa não encontrada com ID: " + id);
                });
    }

    @Transactional
    public Empresa criar(Empresa empresa, UUID idUsuario) {
        logger.info("Criando empresa para usuário ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + idUsuario));

        // Validação: CNPJ único
        if (empresaRepository.existsByCnpj(empresa.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado.");
        }

        empresa.setUsuario(usuario);

        Empresa salva = empresaRepository.save(empresa);
        logger.info("Empresa criada com sucesso. ID: {}", salva.getId());
        return salva;
    }

    @Transactional
    public Empresa atualizar(Integer id, Empresa dadosNovos, UUID idUsuario) {
        logger.info("Atualizando empresa ID: {} para usuário: {}", id, idUsuario);

        Empresa empresa = empresaRepository.findByIdAndUsuarioIdUsuario(id, idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + id));

        // Valida CNPJ apenas se foi alterado
        if (!empresa.getCnpj().equals(dadosNovos.getCnpj())
                && empresaRepository.existsByCnpj(dadosNovos.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado.");
        }

        empresa.setNomeFantasia(dadosNovos.getNomeFantasia());
        empresa.setCnpj(dadosNovos.getCnpj());
        empresa.setSetor(dadosNovos.getSetor());
        empresa.setPorte(dadosNovos.getPorte());
        empresa.setRegimeTributario(dadosNovos.getRegimeTributario());

        Empresa atualizada = empresaRepository.save(empresa);
        logger.info("Empresa atualizada com sucesso. ID: {}", atualizada.getId());
        return atualizada;
    }

    @Transactional
    public void deletar(Integer id, UUID idUsuario) {
        logger.info("Deletando empresa ID: {} para usuário: {}", id, idUsuario);
        Empresa empresa = empresaRepository.findByIdAndUsuarioIdUsuario(id, idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + id));
        empresaRepository.delete(empresa);
        logger.info("Empresa deletada com sucesso. ID: {}", id);
    }
}