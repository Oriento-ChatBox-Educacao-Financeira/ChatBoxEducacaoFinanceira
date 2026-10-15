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

    /**
     * Lista somente as conversas da empresa autenticada (anti-IDOR).
     * O empresaId vem do token JWT via @AuthenticationPrincipal no controller.
     */
    @Transactional(readOnly = true)
    public List<Conversa> listarPorEmpresa(Integer empresaId) {
        logger.debug("Listando conversas da empresa ID: {}", empresaId);
        return conversaRepository.findByEmpresaId(empresaId);
    }

    /**
     * Busca individual com verificação de posse (anti-IDOR).
     * Retorna 404 se o id não existir OU não pertencer à empresa autenticada.
     */
    @Transactional(readOnly = true)
    public Conversa buscarPorId(UUID id, Integer empresaId) {
        logger.debug("Buscando conversa ID: {} para empresa ID: {}", id, empresaId);
        return conversaRepository.findByIdConversaAndEmpresaId(id, empresaId)
                .orElseThrow(() -> {
                    logger.warn("Conversa não encontrada ou não pertence à empresa. ID: {}", id);
                    return new IllegalArgumentException("Conversa não encontrada com ID: " + id);
                });
    }

    /**
     * Cria conversa para o próprio usuário autenticado.
     * userId e empresaId vêm do token JWT — nunca do cliente.
     */
    @Transactional
    public Conversa criar(UUID userId, Integer empresaId) {
        logger.info("Criando conversa para usuário ID: {}, empresa ID: {}", userId, empresaId);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + empresaId));

        Conversa conversa = new Conversa();
        // O UUID é a chave-ponte com o MongoDB — gerado aqui e usado nos dois bancos
        conversa.setIdConversa(UUID.randomUUID());
        conversa.setIdUsuario(userId);
        conversa.setEmpresa(empresa);
        // status e iniciadaEm já têm defaults no model

        Conversa salva = conversaRepository.save(conversa);
        logger.info("Conversa criada com sucesso. ID: {}", salva.getIdConversa());
        return salva;
    }

    /**
     * Encerra uma conversa ativa verificando posse antes de alterar.
     */
    @Transactional
    public Conversa encerrar(UUID id, Integer empresaId) {
        logger.info("Encerrando conversa ID: {}", id);

        Conversa conversa = conversaRepository.findByIdConversaAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada com ID: " + id));

        if (conversa.getStatus() == StatusConversa.encerrada) {
            throw new IllegalArgumentException("Conversa já está encerrada. ID: " + id);
        }

        conversa.setStatus(StatusConversa.encerrada);

        Conversa encerrada = conversaRepository.save(conversa);
        logger.info("Conversa encerrada com sucesso. ID: {}", encerrada.getIdConversa());
        return encerrada;
    }

    /**
     * Deleta conversa verificando posse antes de remover.
     */
    @Transactional
    public void deletar(UUID id, Integer empresaId) {
        logger.info("Deletando conversa ID: {}", id);
        // verificação de posse: lança exceção se não pertencer à empresa
        conversaRepository.findByIdConversaAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada com ID: " + id));
        conversaRepository.deleteById(id);
        logger.info("Conversa deletada com sucesso. ID: {}", id);
    }
}