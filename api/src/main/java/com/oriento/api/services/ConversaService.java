package com.oriento.api.services;

import com.oriento.api.model.Conversa;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.enuns.StatusConversa;
import com.oriento.api.repositories.ConversaRepository;
import com.oriento.api.repositories.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversaService {

    private static final Logger logger = LoggerFactory.getLogger(ConversaService.class);

    private final ConversaRepository conversaRepository;
    private final EmpresaRepository empresaRepository;

    public ConversaService(ConversaRepository conversaRepository,
                           EmpresaRepository empresaRepository) {
        this.conversaRepository = conversaRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<Conversa> listarTodas() {
        logger.debug("Listando todas as conversas");
        return conversaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Conversa> listarPorUsuario(UUID idUsuario) {
        logger.debug("Listando conversas do usuário ID: {}", idUsuario);
        return conversaRepository.findByIdUsuario(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<Conversa> listarPorEmpresa(Integer idEmpresa) {
        logger.debug("Listando conversas da empresa ID: {}", idEmpresa);
        return conversaRepository.findByEmpresaId(idEmpresa);
    }

    @Transactional(readOnly = true)
    public Conversa buscarPorId(UUID id) {
        logger.debug("Buscando conversa ID: {}", id);
        return conversaRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Conversa não encontrada. ID: {}", id);
                    return new IllegalArgumentException("Conversa não encontrada com ID: " + id);
                });
    }

    @Transactional
    public Conversa criar(UUID idUsuario, Integer idEmpresa) {
        logger.info("Criando conversa para usuário ID: {}, empresa ID: {}", idUsuario, idEmpresa);

        Empresa empresa = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + idEmpresa));

        Conversa conversa = new Conversa();
        // O UUID é a chave-ponte com o MongoDB — gerado aqui e usado nos dois bancos
        conversa.setIdConversa(UUID.randomUUID());
        conversa.setIdUsuario(idUsuario);
        conversa.setEmpresa(empresa);
        // status e iniciadaEm já têm defaults no model

        Conversa salva = conversaRepository.save(conversa);
        logger.info("Conversa criada com sucesso. ID: {}", salva.getIdConversa());
        return salva;
    }

    /**
     * Encerra uma conversa ativa.
     * Altera o status de 'ativa' para 'encerrada'.
     */
    @Transactional
    public Conversa encerrar(UUID id) {
        logger.info("Encerrando conversa ID: {}", id);

        Conversa conversa = conversaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada com ID: " + id));

        if (conversa.getStatus() == StatusConversa.encerrada) {
            throw new IllegalArgumentException("Conversa já está encerrada. ID: " + id);
        }

        conversa.setStatus(StatusConversa.encerrada);

        Conversa encerrada = conversaRepository.save(conversa);
        logger.info("Conversa encerrada com sucesso. ID: {}", encerrada.getIdConversa());
        return encerrada;
    }

    @Transactional
    public void deletar(UUID id) {
        logger.info("Deletando conversa ID: {}", id);
        if (!conversaRepository.existsById(id)) {
            throw new IllegalArgumentException("Conversa não encontrada com ID: " + id);
        }
        conversaRepository.deleteById(id);
        logger.info("Conversa deletada com sucesso. ID: {}", id);
    }
}